/**
 * Copyright 2020 Materna Information & Communications SE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.materna.fegen.util.spring.security;

import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

public class SecurityEvaluator {

    protected List<String> httpMethods = Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE");

    public static SecurityEvaluator createInstance(BeanFactory beanFactory) {
        Class<?> webSecurityConfigurationClass = getWebSecurityConfigurationClass();
        if (webSecurityConfigurationClass == null) {
            return new SecurityEvaluator();
        }
        Object webSecurityConfiguration = beanFactory.getBean(webSecurityConfigurationClass);
        try {
            Class<?> impl = Class.forName("de.materna.fegen.util.spring.security.SecurityEvaluatorImpl");
            Constructor<?> constructor = impl.getConstructor(webSecurityConfigurationClass);
            return (SecurityEvaluator) constructor.newInstance(webSecurityConfiguration);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Failed to find SecurityEvaluatorImpl", ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Failed to find constructor for SecurityEvaluatorImpl", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate SecurityEvaluatorImpl", ex);
        }
    }

    private static Class<?> getWebSecurityConfigurationClass() {
        try {
            return Class.forName("org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration");
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    public List<String> allowedMethods(String path) {
        return httpMethods;
    }

    public boolean isAllowed(String path, String method) {
        return true;
    }
}
