package edu.psu.yabt;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import edu.psu.yabt.chart.ChartServiceTest;
import edu.psu.yabt.chart.FlowChartServiceTest;
import edu.psu.yabt.entity.ModelTest;
import edu.psu.yabt.notification.NotificationTest;
import edu.psu.yabt.svn.SVNTest;
import edu.psu.yabt.workflow.EngineTest;
import edu.psu.yabt.workflow.StringUtilsTest;
import edu.psu.yabt.workflow.WorkflowDiagramTest;

@RunWith(Suite.class)
@SuiteClasses({ EngineTest.class, 
				WorkflowDiagramTest.class, 
				StringUtilsTest.class, 
				SVNTest.class,
				NotificationTest.class,
				ModelTest.class,
				ChartServiceTest.class,
				FlowChartServiceTest.class})
public class AllTests {

}
