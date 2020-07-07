package cn.iocoder.mall.systemservice.service.permission;

import cn.iocoder.common.framework.util.ServiceExceptionUtil;
import cn.iocoder.mall.systemservice.convert.permission.ResourceConvert;
import cn.iocoder.mall.systemservice.dal.mysql.dataobject.permission.ResourceDO;
import cn.iocoder.mall.systemservice.dal.mysql.mapper.permission.ResourceMapper;
import cn.iocoder.mall.systemservice.enums.SystemErrorCodeEnum;
import cn.iocoder.mall.systemservice.enums.permission.ResourceIdEnum;
import cn.iocoder.mall.systemservice.enums.permission.ResourceTypeEnum;
import cn.iocoder.mall.systemservice.service.permission.bo.ResourceBO;
import cn.iocoder.mall.systemservice.service.permission.bo.ResourceCreateBO;
import cn.iocoder.mall.systemservice.service.permission.bo.ResourceUpdateBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.mall.systemservice.enums.SystemErrorCodeEnum.*;

/**
* 资源 Service
*/
@Service
@Validated
public class ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    /**
     * 创建资源
     *
     * @param createBO 创建资源 BO
     * @return 资源
     */
    public ResourceBO createResource(@Valid ResourceCreateBO createBO) {
        // 校验父资源存在
        checkParentResource(createBO.getPid(), null);
        // 校验资源（自己）
        checkResource(createBO.getPid(), createBO.getName(), null);
        // 插入数据库
        ResourceDO resourceDO = ResourceConvert.INSTANCE.convert(createBO);
        initResourceProperty(resourceDO);
        resourceMapper.insert(resourceDO);
        // 返回
        return ResourceConvert.INSTANCE.convert(resourceDO);
    }

    /**
     * 更新资源
     *
     * @param updateBO 更新资源 BO
     */
    public void updateResource(@Valid ResourceUpdateBO updateBO) {
        // 校验更新的资源是否存在
        if (resourceMapper.selectById(updateBO.getId()) == null) {
            throw ServiceExceptionUtil.exception(RESOURCE_NOT_EXISTS);
        }
        // 校验父资源存在
        checkParentResource(updateBO.getPid(), updateBO.getId());
        // 校验资源（自己）
        checkResource(updateBO.getPid(), updateBO.getName(), updateBO.getId());
        // 更新到数据库
        ResourceDO updateObject = ResourceConvert.INSTANCE.convert(updateBO);
        initResourceProperty(updateObject);
        resourceMapper.updateById(updateObject);
    }

    /**
     * 删除资源
     *
     * @param resourceId 资源编号
     */
    public void deleteResource(Integer resourceId) {
        // 校验更新的资源是否存在
        if (resourceMapper.selectById(resourceId) == null) {
            throw ServiceExceptionUtil.exception(SystemErrorCodeEnum.RESOURCE_NOT_EXISTS);
        }
        // 校验是否还有子资源
        if (resourceMapper.selectCountByPid(resourceId) > 0) {
            throw ServiceExceptionUtil.exception(SystemErrorCodeEnum.RESOURCE_EXISTS_CHILDREN);
        }
        // 校验删除的资源是否存在
        if (resourceMapper.selectById(resourceId) == null) {
            throw ServiceExceptionUtil.exception(RESOURCE_NOT_EXISTS);
        }
        // 标记删除
        resourceMapper.deleteById(resourceId);
    }

    /**
     * 获得资源
     *
     * @param resourceId 资源编号
     * @return 资源
     */
    public ResourceBO getResource(Integer resourceId) {
        ResourceDO resourceDO = resourceMapper.selectById(resourceId);
        return ResourceConvert.INSTANCE.convert(resourceDO);
    }

    /**
     * 获得资源列表
     *
     * @param resourceIds 资源编号列表
     * @return 资源列表
     */
    public List<ResourceBO> listResource(List<Integer> resourceIds) {
        List<ResourceDO> resourceDOs = resourceMapper.selectBatchIds(resourceIds);
        return ResourceConvert.INSTANCE.convertList(resourceDOs);
    }

    /**
     * 校验父资源是否合法
     *
     * 1. 不能舌质红自己为父资源
     * 2. 父资源不存在
     * 3. 父资源必须是 {@link ResourceTypeEnum#MENU} 菜单类型
     *
     * @param pid 父资源编号
     * @param childId 当前资源编号
     */
    private void checkParentResource(Integer pid, Integer childId) {
        if (pid == null || ResourceIdEnum.ROOT.getId().equals(pid)) {
            return;
        }
        // 不能设置自己为父资源
        if (pid.equals(childId)) {
            throw ServiceExceptionUtil.exception(RESOURCE_PARENT_ERROR);
        }
        ResourceDO resource = resourceMapper.selectById(pid);
        // 父资源不存在
        if (resource == null) {
            throw ServiceExceptionUtil.exception(RESOURCE_PARENT_NOT_EXISTS);
        }
        // 父资源必须是菜单类型
        if (!ResourceTypeEnum.MENU.getType().equals(resource.getType())) {
            throw ServiceExceptionUtil.exception(RESOURCE_PARENT_NOT_MENU);
        }
    }

    /**
     * 校验资源是否合法
     *
     * 1. 校验相同父资源编号下，是否存在相同的资源名
     *
     * @param name 资源名字
     * @param pid 父资源编号
     * @param id 资源编号
     */
    private void checkResource(Integer pid, String name, Integer id) {
        ResourceDO resource = resourceMapper.selectByPidAndName(pid, name);
        if (resource == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的资源
        if (id == null) {
            throw ServiceExceptionUtil.exception(RESOURCE_NAME_DUPLICATE);
        }
        if (!resource.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(RESOURCE_NAME_DUPLICATE);
        }
    }

    /**
     * 初始化资源的通用属性。
     *
     * 例如说，只有菜单类型的资源，才设置 icon
     *
     * @param resource 资源
     */
    private void initResourceProperty(ResourceDO resource) {
        // 初始化资源为按钮类型时，无需 route 和 icon 属性
        if (ResourceTypeEnum.BUTTON.getType().equals(resource.getType())) {
            resource.setRoute(null);
            resource.setIcon(null);
        }
    }

}