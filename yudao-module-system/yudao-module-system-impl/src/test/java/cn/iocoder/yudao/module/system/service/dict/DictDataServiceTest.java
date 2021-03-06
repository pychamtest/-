package cn.iocoder.yudao.module.system.service.dict;

import cn.iocoder.yudao.module.system.dal.dataobject.dict.DictDataDO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.data.DictDataCreateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.data.DictDataExportReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.data.DictDataPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.data.DictDataUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dict.DictTypeDO;
import cn.iocoder.yudao.module.system.dal.mysql.dict.DictDataMapper;
import cn.iocoder.yudao.module.system.mq.producer.dict.DictDataProducer;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.google.common.collect.ImmutableTable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static cn.hutool.core.bean.BeanUtil.getFieldValue;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import(DictDataServiceImpl.class)
public class DictDataServiceTest extends BaseDbUnitTest {

    @Resource
    private DictDataServiceImpl dictDataService;

    @Resource
    private DictDataMapper dictDataMapper;
    @MockBean
    private DictTypeService dictTypeService;
    @MockBean
    private DictDataProducer dictDataProducer;

    /**
     * ??????????????????????????????????????????
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testInitLocalCache() {
        // mock ??????
        DictDataDO dictData01 = randomDictDataDO();
        dictDataMapper.insert(dictData01);
        DictDataDO dictData02 = randomDictDataDO();
        dictDataMapper.insert(dictData02);

        // ??????
        dictDataService.initLocalCache();
        // ?????? labelDictDataCache ??????
        ImmutableTable<String, String, DictDataDO> labelDictDataCache =
                (ImmutableTable<String, String, DictDataDO>) getFieldValue(dictDataService, "labelDictDataCache");
        assertEquals(2, labelDictDataCache.size());
        assertPojoEquals(dictData01, labelDictDataCache.get(dictData01.getDictType(), dictData01.getLabel()));
        assertPojoEquals(dictData02, labelDictDataCache.get(dictData02.getDictType(), dictData02.getLabel()));
        // ?????? valueDictDataCache ??????
        ImmutableTable<String, String, DictDataDO> valueDictDataCache =
                (ImmutableTable<String, String, DictDataDO>) getFieldValue(dictDataService, "valueDictDataCache");
        assertEquals(2, valueDictDataCache.size());
        assertPojoEquals(dictData01, valueDictDataCache.get(dictData01.getDictType(), dictData01.getValue()));
        assertPojoEquals(dictData02, valueDictDataCache.get(dictData02.getDictType(), dictData02.getValue()));
        // ?????? maxUpdateTime ??????
        Date maxUpdateTime = (Date) getFieldValue(dictDataService, "maxUpdateTime");
        assertEquals(ObjectUtils.max(dictData01.getUpdateTime(), dictData02.getUpdateTime()), maxUpdateTime);
    }

    @Test
    public void testGetDictDataPage() {
        // mock ??????
        DictDataDO dbDictData = randomPojo(DictDataDO.class, o -> { // ???????????????
            o.setLabel("??????");
            o.setDictType("yunai");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        dictDataMapper.insert(dbDictData);
        // ?????? label ?????????
        dictDataMapper.insert(ObjectUtils.cloneIgnoreId(dbDictData, o -> o.setLabel("???")));
        // ?????? dictType ?????????
        dictDataMapper.insert(ObjectUtils.cloneIgnoreId(dbDictData, o -> o.setDictType("nai")));
        // ?????? status ?????????
        dictDataMapper.insert(ObjectUtils.cloneIgnoreId(dbDictData, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // ????????????
        DictDataPageReqVO reqVO = new DictDataPageReqVO();
        reqVO.setLabel("???");
        reqVO.setDictType("yu");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        // ??????
        PageResult<DictDataDO> pageResult = dictDataService.getDictDataPage(reqVO);
        // ??????
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbDictData, pageResult.getList().get(0));
    }

    @Test
    public void testGetDictDataList() {
        // mock ??????
        DictDataDO dbDictData = randomPojo(DictDataDO.class, o -> { // ???????????????
            o.setLabel("??????");
            o.setDictType("yunai");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        dictDataMapper.insert(dbDictData);
        // ?????? label ?????????
        dictDataMapper.insert(ObjectUtils.cloneIgnoreId(dbDictData, o -> o.setLabel("???")));
        // ?????? dictType ?????????
        dictDataMapper.insert(ObjectUtils.cloneIgnoreId(dbDictData, o -> o.setDictType("nai")));
        // ?????? status ?????????
        dictDataMapper.insert(ObjectUtils.cloneIgnoreId(dbDictData, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // ????????????
        DictDataExportReqVO reqVO = new DictDataExportReqVO();
        reqVO.setLabel("???");
        reqVO.setDictType("yu");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        // ??????
        List<DictDataDO> list = dictDataService.getDictDatas(reqVO);
        // ??????
        assertEquals(1, list.size());
        assertPojoEquals(dbDictData, list.get(0));
    }

    @Test
    public void testCreateDictData_success() {
        // ????????????
        DictDataCreateReqVO reqVO = randomPojo(DictDataCreateReqVO.class,
                o -> o.setStatus(randomCommonStatus()));
        // mock ??????
        when(dictTypeService.getDictType(eq(reqVO.getDictType()))).thenReturn(randomDictTypeDO(reqVO.getDictType()));

        // ??????
        Long dictDataId = dictDataService.createDictData(reqVO);
        // ??????
        assertNotNull(dictDataId);
        // ?????????????????????????????????
        DictDataDO dictData = dictDataMapper.selectById(dictDataId);
        assertPojoEquals(reqVO, dictData);
        // ????????????
        verify(dictDataProducer, times(1)).sendDictDataRefreshMessage();
    }

    @Test
    public void testUpdateDictData_success() {
        // mock ??????
        DictDataDO dbDictData = randomDictDataDO();
        dictDataMapper.insert(dbDictData);// @Sql: ?????????????????????????????????
        // ????????????
        DictDataUpdateReqVO reqVO = randomPojo(DictDataUpdateReqVO.class, o -> {
            o.setId(dbDictData.getId()); // ??????????????? ID
            o.setStatus(randomCommonStatus());
        });
        // mock ?????????????????????
        when(dictTypeService.getDictType(eq(reqVO.getDictType()))).thenReturn(randomDictTypeDO(reqVO.getDictType()));

        // ??????
        dictDataService.updateDictData(reqVO);
        // ????????????????????????
        DictDataDO dictData = dictDataMapper.selectById(reqVO.getId()); // ???????????????
        assertPojoEquals(reqVO, dictData);
        // ????????????
        verify(dictDataProducer, times(1)).sendDictDataRefreshMessage();
    }

    @Test
    public void testDeleteDictData_success() {
        // mock ??????
        DictDataDO dbDictData = randomDictDataDO();
        dictDataMapper.insert(dbDictData);// @Sql: ?????????????????????????????????
        // ????????????
        Long id = dbDictData.getId();

        // ??????
        dictDataService.deleteDictData(id);
        // ????????????????????????
        assertNull(dictDataMapper.selectById(id));
        // ????????????
        verify(dictDataProducer, times(1)).sendDictDataRefreshMessage();
    }

    @Test
    public void testCheckDictDataExists_success() {
        // mock ??????
        DictDataDO dbDictData = randomDictDataDO();
        dictDataMapper.insert(dbDictData);// @Sql: ?????????????????????????????????

        // ????????????
        dictDataService.checkDictDataExists(dbDictData.getId());
    }

    @Test
    public void testCheckDictDataExists_notExists() {
        assertServiceException(() -> dictDataService.checkDictDataExists(randomLongId()), DICT_DATA_NOT_EXISTS);
    }

    @Test
    public void testCheckDictTypeValid_success() {
        // mock ??????????????????????????????
        String type = randomString();
        when(dictTypeService.getDictType(eq(type))).thenReturn(randomDictTypeDO(type));

        // ??????, ??????
        dictDataService.checkDictTypeValid(type);
    }

    @Test
    public void testCheckDictTypeValid_notExists() {
        assertServiceException(() -> dictDataService.checkDictTypeValid(randomString()), DICT_TYPE_NOT_EXISTS);
    }

    @Test
    public void testCheckDictTypeValid_notEnable() {
        // mock ??????????????????????????????
        String dictType = randomString();
        when(dictTypeService.getDictType(eq(dictType))).thenReturn(
                randomPojo(DictTypeDO.class, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));

        // ??????, ???????????????
        assertServiceException(() -> dictDataService.checkDictTypeValid(dictType), DICT_TYPE_NOT_ENABLE);
    }

    @Test
    public void testCheckDictDataValueUnique_success() {
        // ???????????????
        dictDataService.checkDictDataValueUnique(randomLongId(), randomString(), randomString());
    }

    @Test
    public void testCheckDictDataValueUnique_valueDuplicateForCreate() {
        // ????????????
        String dictType = randomString();
        String value = randomString();
        // mock ??????
        dictDataMapper.insert(randomDictDataDO(o -> {
            o.setDictType(dictType);
            o.setValue(value);
        }));

        // ?????????????????????
        assertServiceException(() -> dictDataService.checkDictDataValueUnique(null, dictType, value),
                DICT_DATA_VALUE_DUPLICATE);
    }

    @Test
    public void testCheckDictDataValueUnique_valueDuplicateForUpdate() {
        // ????????????
        Long id = randomLongId();
        String dictType = randomString();
        String value = randomString();
        // mock ??????
        dictDataMapper.insert(randomDictDataDO(o -> {
            o.setDictType(dictType);
            o.setValue(value);
        }));

        // ?????????????????????
        assertServiceException(() -> dictDataService.checkDictDataValueUnique(id, dictType, value),
                DICT_DATA_VALUE_DUPLICATE);
    }

    // ========== ???????????? ==========

    @SafeVarargs
    private static DictDataDO randomDictDataDO(Consumer<DictDataDO>... consumers) {
        Consumer<DictDataDO> consumer = (o) -> {
            o.setStatus(randomCommonStatus()); // ?????? status ?????????
        };
        return randomPojo(DictDataDO.class, ArrayUtils.append(consumer, consumers));
    }

    /**
     * ?????????????????????????????????
     *
     * @param type ????????????
     * @return DictTypeDO ??????
     */
    private static DictTypeDO randomDictTypeDO(String type) {
        return randomPojo(DictTypeDO.class, o -> {
            o.setType(type);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus()); // ?????? status ?????????
        });
    }

}
