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

import java.util.HashMap;

public class BeanMetric {
    private final String beanName;
    private long elapsedBeanCreationTime;
    private HashMap<String, BeanMetric> dependencies = new HashMap<String, BeanMetric>();

    public BeanMetric(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }

    public long getElapsedBeanCreationTime() {
        long aggregatedTime = elapsedBeanCreationTime;
        for (BeanMetric dependency : dependencies.values()) {
            aggregatedTime -= dependency.getElapsedBeanCreationTime();
        }
        return aggregatedTime;
    }

    public void setElapsedBeanCreationTime(long buildTime) {
        this.elapsedBeanCreationTime = buildTime;
    }

    public void addDependency(BeanMetric dependency) {
        dependencies.put(dependency.getBeanName(), dependency);
    }

}
