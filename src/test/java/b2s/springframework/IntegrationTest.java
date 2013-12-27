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
package b2s.springframework;

import b2s.springframework.bean.PausingBean;
import b2s.springframework.bean.PausingInitializingBean;
import b2s.springframework.bean.annotation.PausingPostConstructBean;
import b2s.springframework.metric.BeanMetric;
import b2s.springframework.metric.MetricCapturingListableBeanFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class IntegrationTest {

    private MetricCapturingListableBeanFactory metricCapturingListableBeanFactory;

    @Before
    public void setUp() throws Exception {
        metricCapturingListableBeanFactory = new MetricCapturingListableBeanFactory();
    }

    @Test
    public void shouldKeepTrackOfJavaConfigurationBeans() {
        PausingBean.creationTimes(30, 80, 50);

        loadContext("classpath:java-configuration-beans.xml");

        List<BeanMetric> beanMetrics = metricCapturingListableBeanFactory.getBeanMetrics();

        assertLoadTimeFor("bean-1", 56, beanMetrics);
        assertLoadTimeFor("bean-2", 80, beanMetrics);
        assertLoadTimeFor("bean-3", 50, beanMetrics);
    }

    @Test
    public void shouldKeepTrackOfInitializingBeans() {
        PausingInitializingBean.initializingTimes(30, 80, 50);

        loadContext("classpath:initializing-beans.xml");

        List<BeanMetric> beanMetrics = metricCapturingListableBeanFactory.getBeanMetrics();

        assertLoadTimeFor("bean-1", 56, beanMetrics);
        assertLoadTimeFor("bean-2", 80, beanMetrics);
        assertLoadTimeFor("bean-3", 50, beanMetrics);
    }

    @Test
    public void shouldKeepTrackOfPostConstructTimes() {
        PausingPostConstructBean.constructTimes(30, 80, 50);

        loadContext("classpath:post-construct-beans.xml");

        List<BeanMetric> beanMetrics = metricCapturingListableBeanFactory.getBeanMetrics();

        assertLoadTimeFor("bean-1", 56, beanMetrics);
        assertLoadTimeFor("bean-2", 80, beanMetrics);
        assertLoadTimeFor("bean-3", 50, beanMetrics);
    }

    @Test
    public void shouldHandleBeansWhenTheyAreDeeplyNested() {
        PausingBean.creationTimes(30, 80, 50, 10);

        loadContext("classpath:beans-with-multiple-nesting.xml");

        List<BeanMetric> beanMetrics = metricCapturingListableBeanFactory.getBeanMetrics();

        assertLoadTimeFor("bean-1", 100, beanMetrics);
        assertLoadTimeFor("bean-2", 80, beanMetrics);
        assertLoadTimeFor("bean-3", 100, beanMetrics);
        assertLoadTimeFor("bean-4", 10, beanMetrics);
    }

    @Test
    public void shouldRemoveChildrenLoadTimeFromTheParentBeanLoadTimeToGetTheActualBuildTimeOfABean() {
        PausingBean.creationTimes(500, 200, 100);

        loadContext("classpath:bean-with-dependencies.xml");

        List<BeanMetric> beanMetrics = metricCapturingListableBeanFactory.getBeanMetrics();
        assertLoadTimeFor("bean-1", 520, beanMetrics);
        assertLoadTimeFor("bean-2", 200, beanMetrics);
        assertLoadTimeFor("bean-3", 100, beanMetrics);
    }

    @Test
    public void shouldAllowGettingTheBeansWithTheirTimeItTookToLoad() {
        PausingBean.creationTimes(500, 200, 100, 40, 50);

        loadContext("classpath:isolated-beans.xml");

        List<BeanMetric> beanMetrics = metricCapturingListableBeanFactory.getBeanMetrics();
        assertLoadTimeFor("bean-1", 520, beanMetrics);
        assertLoadTimeFor("bean-2", 200, beanMetrics);
        assertLoadTimeFor("bean-3", 100, beanMetrics);
        assertLoadTimeFor("bean-4", 40, beanMetrics);
        assertLoadTimeFor("bean-5", 50, beanMetrics);
    }

    @Test
    public void shouldKeepTrackOfTheNumberOfBeansLoaded() {
        loadContext("classpath:isolated-beans.xml");

        assertEquals(5, metricCapturingListableBeanFactory.getNumberOfBeansLoaded());
    }

    private void loadContext(final String configLocation) {
        new ClassPathXmlApplicationContext(configLocation) {
            @Override
            protected DefaultListableBeanFactory createBeanFactory() {
                return metricCapturingListableBeanFactory;
            }
        };
    }

    private void assertApproximateBeanLoadTime(int expectedLoadTime, BeanMetric beanMetric) {
        final int offset = 60;
        int min = expectedLoadTime - offset;
        int max = expectedLoadTime + offset;
        if (min >= beanMetric.getElapsedBeanCreationTime() || max <= beanMetric.getElapsedBeanCreationTime()) {
            fail(beanMetric.getBeanName() + " failed to assert within load time range: " + min + " > " + beanMetric.getElapsedBeanCreationTime() + " > " + max+", we were expecting approx. "+expectedLoadTime+" millis");
        }
    }

    private void assertLoadTimeFor(String beanId, int expectedLoadTime, List<BeanMetric> actualMetrics) {
        for (BeanMetric metric : actualMetrics) {
            if (beanId.equals(metric.getBeanName())) {
                assertApproximateBeanLoadTime(expectedLoadTime, metric);
                break;
            }
        }
    }
}
