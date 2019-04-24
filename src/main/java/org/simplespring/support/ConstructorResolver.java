package org.simplespring.support;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.simplespring.config.Bean;
import org.simplespring.config.ConstructorArg;
import org.simplespring.main.BeanFactory;
import org.simplespring.util.AutowireUtils;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

public class ConstructorResolver
{

    /**
     * 搜索匹配的构造函数及其参数
     */
    public static ArgumentsHolder matchConstructor(Bean beanInfo, Class beanClass, BeanFactory beanFactory)
    {
        Constructor<?>[] constructors = beanClass.getConstructors();
        AutowireUtils.sortConstructors(constructors); // 对构造函数们按照 public 在前，参数个数多的在前的顺序进行排序
        int paramNum = beanInfo.getIndexConstructorArgs().size() + beanInfo.getGenericConstructorArgs().size();

        // 遍历所有的构造函数进行匹配，有匹配成功的则返回
        for (Constructor<?> candidate : constructors)
        {
            Class<?>[] paramTypes = candidate.getParameterTypes();//反射自带函数
            Object[] argsToUse = new Object[paramNum];

            if (paramTypes.length > paramNum)
            {
                continue;
            } else if (paramTypes.length < paramNum)
            {
                break;
            }

            boolean found = true;
            Set<ConstructorArg> usedConstructorArg = new HashSet<>(paramNum);
            ConstructorArg constructorArg = null;
            ConvertUtilsBean converter = new ConvertUtilsBean();
            for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++)
            {
                Class<?> paramType = paramTypes[paramIndex];
                // 先通过 index
                constructorArg = beanInfo.getArgumentValue(paramIndex, paramType, usedConstructorArg);

                if (constructorArg == null)
                {
                    constructorArg = beanInfo.getGenericArgumentValue(usedConstructorArg);
                }

                if (constructorArg != null)
                {
                    usedConstructorArg.add(constructorArg);
                    String originalValueStr = constructorArg.getValueStr();
                    Object convertObj = null;
                    if (constructorArg.isConvert())
                    {
                        try {
                            convertObj = converter.convert(originalValueStr, paramType);
                        } catch (ConversionException e) {
                            found = false;
                            break;
                        }
                    } else

                        {
                        convertObj = beanFactory.getBean(originalValueStr);
                        if (!paramType.isInstance(convertObj))
                        {
                            found = false;
                            break;
                        }
                    }
                    if (convertObj != null)
                    {
                        argsToUse[paramIndex] = convertObj;
                    }
                }
            }

            if (found)
            {
                return new ArgumentsHolder(candidate, argsToUse);
            }
        }
        return null;
    }

    public static class ArgumentsHolder
    {
        public final Constructor<?> constructor;
        public final Object[] arguments;

        public ArgumentsHolder(Constructor<?> constructor, Object[] arguments)
        {
            this.arguments = arguments;
            this.constructor = constructor;
        }

        public Object[] getArguments() {
            return arguments;
        }

        public Constructor<?> getConstructor() {
            return constructor;
        }
    }

}
