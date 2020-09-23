package org.opsli.api.utils;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReflectUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opsli.api.msg.ValidationMsg;
import org.opsli.api.wrapper.system.dict.SysDictModel;
import org.opsli.common.annotation.validation.ValidationArgs;
import org.opsli.common.annotation.validation.ValidationArgsMax;
import org.opsli.common.annotation.validation.ValidationArgsMin;
import org.opsli.common.enums.ValiArgsType;
import org.opsli.common.exception.ServiceException;

import java.lang.reflect.Field;

/**
 * @BelongsProject: opsli-boot
 * @BelongsPackage: org.opsli.common.utils
 * @Author: Parker
 * @CreateTime: 2020-09-22 17:29
 * @Description: 验证器工具类
 */
@Slf4j
public final class ValidationUtil {

    /** 编码格式 */
    private static final String CHARSET_NAME = "utf-8";

    /**
     * 验证对象
     * @param obj
     */
    public static void verify(Object obj){
        if(obj == null){
            return;
        }

        Field[] fields = ReflectUtil.getFields(obj.getClass());
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            // 获得 统一验证 注解
            ValidationArgs validationArgs = field.getAnnotation(ValidationArgs.class);
            if(validationArgs != null){
                ValiArgsType[] types = validationArgs.value();
                Object fieldValue = ReflectUtil.getFieldValue(obj, field);
                ValidationUtil.check(field, types, fieldValue);
            }

            // 获得 最大长度 注解
            ValidationArgsMax validationArgsMax = field.getAnnotation(ValidationArgsMax.class);
            if(validationArgsMax != null){
                int maxLength = validationArgsMax.value();
                Object fieldValue = ReflectUtil.getFieldValue(obj, field);
                ValidationUtil.checkMax(field, maxLength, fieldValue);
            }

            // 获得 最小长度 注解
            ValidationArgsMin validationArgsMin = field.getAnnotation(ValidationArgsMin.class);
            if(validationArgsMin != null){
                int minLength = validationArgsMin.value();
                Object fieldValue = ReflectUtil.getFieldValue(obj, field);
                ValidationUtil.checkMin(field, minLength, fieldValue);
            }
        }
    }

    /**
     * 通用校验
     * @param field
     * @param types
     * @param fieldValue
     */
    private static void check(Field field, ValiArgsType[] types, Object fieldValue){
        // 获得字段名
        String fieldName = field.getName();
        ApiModelProperty annotation = field.getAnnotation(ApiModelProperty.class);
        if(annotation != null){
            fieldName = annotation.value();
        }

        String value = String.valueOf(fieldValue);

        // 循环验证
        for (ValiArgsType type : types) {
            try {
                switch (type) {
                    // 不能为空
                    case IS_NOT_NULL:
                        boolean notNull = Validator.isNotEmpty(fieldValue);
                        if(!notNull){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_NOT_NULL;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 字母，数字和下划线
                    case IS_GENERAL:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean general = Validator.isGeneral(value);
                        if(!general){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_GENERAL;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 数字
                    case IS_NUMBER:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean number = Validator.isNumber(value);
                        if(!number){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_NUMBER;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 纯字母
                    case IS_LETTER:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean letter = Validator.isLetter(value);
                        if(!letter){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_LETTER;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 大写
                    case IS_UPPER_CASE:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean upperCase = Validator.isUpperCase(value);
                        if(!upperCase){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_UPPER_CASE;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 小写
                    case IS_LOWER_CASE:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean lowerCase = Validator.isLowerCase(value);
                        if(!lowerCase){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_LOWER_CASE;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // IPV4
                    case IS_IPV4:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean ipv4 = Validator.isIpv4(value);
                        if(!ipv4){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_IPV4;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 金额
                    case IS_MONEY:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean money = Validator.isMoney(value);
                        if(!money){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_MONEY;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 邮箱
                    case IS_EMAIL:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean email = Validator.isEmail(value);
                        if(!email){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_EMAIL;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 手机号
                    case IS_MOBILE:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean mobile = Validator.isMobile(value);
                        if(!mobile){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_MOBILE;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 18位身份证
                    case IS_CITIZENID:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean citizenId = Validator.isCitizenId(value);
                        if(!citizenId){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_CITIZENID;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 邮编
                    case IS_ZIPCODE:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean zipCode = Validator.isZipCode(value);
                        if(!zipCode){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_ZIPCODE;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // URL
                    case IS_URL:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean url = Validator.isUrl(value);
                        if(!url){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_URL;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 汉字
                    case IS_CHINESE:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean chinese = Validator.isChinese(value);
                        if(!chinese){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_CHINESE;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 汉字，字母，数字和下划线
                    case IS_GENERAL_WITH_CHINESE:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean generalWithChinese = Validator.isGeneralWithChinese(value);
                        if(!generalWithChinese){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_GENERAL_WITH_CHINESE;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // MAC地址
                    case IS_MAC:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean mac = Validator.isMac(value);
                        if(!mac){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_MAC;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    // 中国车牌
                    case IS_PLATE_NUMBER:
                        if(StringUtils.isEmpty(value) || "null".equals(value)) break;
                        boolean plateNumber = Validator.isPlateNumber(value);
                        if(!plateNumber){
                            ValidationMsg msg = ValidationMsg.EXCEPTION_IS_PLATE_NUMBER;
                            msg.setFieldName(fieldName);
                            throw new ServiceException(msg);
                        }
                        break;
                    default:
                        break;
                }
            }catch (ServiceException e){
                throw e;
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        }
    }


    /**
     * 最大长度校验
     * @param field
     * @param maxLength
     * @param fieldValue
     */
    private static void checkMax(Field field, int maxLength, Object fieldValue){
        // 获得字段名
        String fieldName = field.getName();
        ApiModelProperty annotation = field.getAnnotation(ApiModelProperty.class);
        if(annotation != null){
            fieldName = annotation.value();
        }

        // 循环验证
        try {
            String value = String.valueOf(fieldValue);
            if(value != null){
                // 转换为 数据库真实 长度
                int strLength = value.getBytes(CHARSET_NAME).length;
                if(strLength > maxLength){
                    ValidationMsg msg = ValidationMsg.EXCEPTION_IS_MAX;
                    msg.setFieldName(fieldName);
                    throw new ServiceException(msg);
                }
            }
        }catch (ServiceException e){
            throw e;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }

    /**
     * 最小长度校验
     * @param field
     * @param minLength
     * @param fieldValue
     */
    private static void checkMin(Field field, int minLength, Object fieldValue){
        // 获得字段名
        String fieldName = field.getName();
        ApiModelProperty annotation = field.getAnnotation(ApiModelProperty.class);
        if(annotation != null){
            fieldName = annotation.value();
        }

        // 循环验证
        try {
            String value = String.valueOf(fieldValue);
            if(value != null){
                // 转换为 数据库真实 长度
                int strLength = value.getBytes(CHARSET_NAME).length;
                if(strLength < minLength){
                    ValidationMsg msg = ValidationMsg.EXCEPTION_IS_MIN;
                    msg.setFieldName(fieldName);
                    throw new ServiceException(msg);
                }
            }
        }catch (ServiceException e){
            throw e;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }

    public static void main(String[] args) {
        SysDictModel sysDictModel = new SysDictModel();
        sysDictModel.setTypeCode("asdsa");
        sysDictModel.setTypeName("阿哈哈哈哈");
        sysDictModel.setRemark("测试11232131231231223123");
        sysDictModel.setIzLock('1');

        ValidationUtil.verify(sysDictModel);
    }


    // ================
    private ValidationUtil(){}
}
