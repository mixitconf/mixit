package mixit.support

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.AbstractApplicationContext
import kotlin.reflect.KClass

fun <T : Any> AbstractApplicationContext.getBean(type: KClass<T>) = getBean(type.java)

fun AnnotationConfigApplicationContext.register(type: KClass<*>) {
    register(type.java)
}

fun DefaultListableBeanFactory.register(beanName: String, type: KClass<*>, propertyName: String, properyValue: Any) {
    registerBeanDefinition(beanName, genericBeanDefinition(type.java).addPropertyValue(propertyName, properyValue).setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR).setScope(BeanDefinition.SCOPE_SINGLETON).beanDefinition)
}
