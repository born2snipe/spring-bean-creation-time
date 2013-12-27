/**
 *
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package b2s.springframework.metric;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MetricCapturingListableBeanFactory extends DefaultListableBeanFactory {
    private ArrayList<String> beanNesting = new ArrayList<String>();
    private LinkedHashMap<String, BeanMetric> beanIdToMetric = new LinkedHashMap<String, BeanMetric>();

    @Override
    protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
        long start = System.currentTimeMillis();
        BeanMetric metric = new BeanMetric(beanName);
        beanIdToMetric.put(beanName, metric);
        try {
            if (isDependencyOfAnotherBean()) {
                findParentBean().addDependency(metric);
            }
            beanNesting.add(beanName);
            return super.doCreateBean(beanName, mbd, args);
        } finally {
            beanIdToMetric.get(beanName).setElapsedBeanCreationTime(System.currentTimeMillis() - start);
            beanNesting.remove(beanName);
        }
    }

    private BeanMetric findParentBean() {
        String beanId = beanNesting.get(beanNesting.size() - 1);
        return beanIdToMetric.get(beanId);
    }

    private boolean isDependencyOfAnotherBean() {
        return beanNesting.size() > 0;
    }

    public int getNumberOfBeansLoaded() {
        return beanIdToMetric.size();
    }

    public List<BeanMetric> getBeanMetrics() {
        return new ArrayList<BeanMetric>(beanIdToMetric.values());
    }
}
