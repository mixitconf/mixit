package fr.mixit.support

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import kotlin.reflect.KClass

fun <T : Any> DefaultListableBeanFactory.register(type: KClass<T>) {
    var className = type.simpleName!!
    var beanName = className.substring(0, 1).toLowerCase() + className.substring(1)
    registerBeanDefinition(beanName, genericBeanDefinition(type.java).setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR).setScope(BeanDefinition.SCOPE_SINGLETON).beanDefinition)
}



