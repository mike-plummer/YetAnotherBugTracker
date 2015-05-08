package edu.psu.yabt.workflow;

import static org.junit.Assert.*;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.psu.yabt.bean.WorkflowBean;

public class WorkflowDiagramTest {

	static ProcessEngine engine;
	
	@BeforeClass
	public static void createEngine()
	{
		if( ProcessEngines.getDefaultProcessEngine() != null )
		{
			engine = ProcessEngines.getDefaultProcessEngine();
		}
		else
		{
			engine = ProcessEngineConfiguration
	        	    .createProcessEngineConfigurationFromResourceDefault()
	        	    .buildProcessEngine();
		}
		if( engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("Requirement").list().isEmpty() )
		{
			engine.getRepositoryService().createDeployment()
			  .addInputStream("Requirement.bpmn", EngineTest.class.getResourceAsStream("/edu/psu/yabt/workflows/Requirement.bpmn"))
			  .deploy();
		}
	}
	
	@Test
	public void testGetProcessDiagram() throws Exception
	{
		ProcessDefinition procDef = engine.getRepositoryService().createProcessDefinitionQuery().list().get(0);
		InputStream stream = new WorkflowBean(engine).buildProcessDiagram(procDef.getName());
		assertTrue(stream.available() > 10000);
		//writeImage(stream, "img.png");  //Uncomment to write out image to file if you want to verify what it looks like
	}
	
	@Test
	public void testGetCurrentProcessDiagram() throws Exception
	{
		RuntimeService runtimeService = engine.getRuntimeService();
		String procDefId = engine.getRepositoryService().createProcessDefinitionQuery().list().get(0).getId();
		ProcessInstance inst = runtimeService.startProcessInstanceById(procDefId, "YABT-0002");
		InputStream stream = new WorkflowBean(engine).buildProcessInstanceDiagram(inst.getBusinessKey());
		assertTrue(stream.available() > 10000);
		//writeImage(stream, "img2.png");	//Uncomment to write out image to file if you want to verify what it looks like
	}
	
	private void writeImage(InputStream stream, String filename) throws IOException
	{
		System.out.println(stream.available()+" bytes");
		Iterator<?> readers = ImageIO.getImageReadersByFormatName("png");
        ImageReader reader = (ImageReader) readers.next();
        ImageInputStream iis = ImageIO.createImageInputStream(stream);
        reader.setInput(iis, true);
        ImageReadParam param = reader.getDefaultReadParam();
        Image image = reader.read(0, param);
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, null, null);
        File imageFile = new File(filename);
        System.out.println(imageFile.getCanonicalPath());
        imageFile.createNewFile();
        ImageIO.write(bufferedImage, "png", imageFile);
	}
}
