package ru.open.monitor.statistics.item;

import static ru.open.monitor.statistics.item.ItemUtil.getCleanClassName;
import static ru.open.monitor.statistics.item.ItemUtil.getSimpleClassName;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemUtilTest {
    private static final Logger LOG = LoggerFactory.getLogger(ItemUtilTest.class);

    @Test
    public void testCleanClassNameSpringCGLib() throws Exception {
        String sourceClassName = "ru.open.haven.risk.service.riskjournal.RiskJournalService$$EnhancerBySpringCGLIB$$9d0e1d4b";
        LOG.info("Source class name: {}", sourceClassName);
        String cleanClassName = getCleanClassName(sourceClassName);
        LOG.info("Clean class name: {}", cleanClassName);

        Assert.assertEquals(cleanClassName, "ru.open.haven.risk.service.riskjournal.RiskJournalService");
    }

    @Test
    public void testCleanClassNameCGLib() throws Exception {
        String sourceClassName = "ru.open.haven.risk.service.riskjournal.RiskJournalService$$EnhancerByCGLIB$$813937ba";
        LOG.info("Source class name: {}", sourceClassName);
        String cleanClassName = getCleanClassName(sourceClassName);
        LOG.info("Clean class name: {}", cleanClassName);

        Assert.assertEquals(cleanClassName, "ru.open.haven.risk.service.riskjournal.RiskJournalService");
    }

    @Test
    public void testCleanCassNameProxy() throws Exception {
        String sourceClassName = "ru.open.haven.risk.service.riskjournal.RiskJournalService$Proxy282";
        LOG.info("Source class name: {}", sourceClassName);
        String cleanClassName = getCleanClassName(sourceClassName);
        LOG.info("Clean class name: {}", cleanClassName);

        Assert.assertEquals(cleanClassName, "ru.open.haven.risk.service.riskjournal.RiskJournalService");
    }

    @Test
    public void testCleanClassName() throws Exception {
        String sourceClassName = "ru.open.haven.risk.service.riskjournal.RiskJournalService";
        LOG.info("Source class name: {}", sourceClassName);
        String cleanClassName = getCleanClassName(sourceClassName);
        LOG.info("Clean class name: {}", cleanClassName);

        Assert.assertEquals(cleanClassName, "ru.open.haven.risk.service.riskjournal.RiskJournalService");
    }

    @Test
    public void testSimpleClassName() throws Exception {
        String sourceClassName = "ru.open.haven.risk.service.riskjournal.RiskJournalService";
        LOG.info("Source class name: {}", sourceClassName);
        String simpleClassName = getSimpleClassName(sourceClassName);
        LOG.info("Simple class name: {}", simpleClassName);

        Assert.assertEquals(simpleClassName, "RiskJournalService");
    }

}
