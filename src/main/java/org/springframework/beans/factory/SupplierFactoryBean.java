/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory;

import java.util.function.Supplier;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * TODO Replace by functional bean registration API when available, see https://jira.spring.io/browse/SPR-14832
 */
public class SupplierFactoryBean extends AbstractFactoryBean<Object> implements ApplicationContextAware, EnvironmentAware, BeanNameAware {

	private Class<?> type;

	private Supplier<?> supplier;

	private ApplicationContext applicationContext;

	private ClassLoader beanClassLoader;

	private Environment environment;

	private String beanName;

	public SupplierFactoryBean(Class<?> type, Supplier<?> supplier) {
		this.type = type;
		this.supplier = supplier;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		super.setBeanClassLoader(beanClassLoader);
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public Class<?> getObjectType() {
		return type;
	}

	@Override
	protected Object createInstance() throws Exception {
		Object instance = supplier.get();
		if (instance instanceof ApplicationContextAware) {
			((ApplicationContextAware)instance).setApplicationContext(this.applicationContext);
		}
		if (instance instanceof BeanClassLoaderAware) {
			((BeanClassLoaderAware)instance).setBeanClassLoader(this.beanClassLoader);
		}
		if (instance instanceof EnvironmentAware) {
			((EnvironmentAware)instance).setEnvironment(this.environment);
		}
		if (instance instanceof BeanNameAware) {
			((BeanNameAware)instance).setBeanName(this.beanName);
		}
		if (instance instanceof BeanFactoryAware) {
			((BeanFactoryAware)instance).setBeanFactory(getBeanFactory());
		}
		if (instance instanceof InitializingBean) {
			((InitializingBean)instance).afterPropertiesSet();
		}
		return instance;
	}
}
