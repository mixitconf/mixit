package mixit.support

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.support.AbstractApplicationContext
import kotlin.reflect.KClass

fun <T : Any> AbstractApplicationContext.getBean(type: KClass<T>) = getBean(type.java)


fun DefaultListableBeanFactory.register(type: KClass<*>) {
    var className = type.simpleName!!
    var beanName = className.substring(0, 1).toLowerCase() + className.substring(1)
    registerBeanDefinition(beanName, genericBeanDefinition(type.java).setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR).setScope(BeanDefinition.SCOPE_SINGLETON).beanDefinition)
}

fun DefaultListableBeanFactory.register(type: KClass<*>, propertyName: String, properyValue: Any) {
    var className = type.simpleName!!
    var beanName = className.substring(0, 1).toLowerCase() + className.substring(1)
    registerBeanDefinition(beanName, genericBeanDefinition(type.java).addPropertyValue(propertyName, properyValue).setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR).setScope(BeanDefinition.SCOPE_SINGLETON).beanDefinition)
}



