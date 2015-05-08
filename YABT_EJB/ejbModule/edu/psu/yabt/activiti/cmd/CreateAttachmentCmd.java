package edu.psu.yabt.activiti.cmd;

import java.io.InputStream;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.AttachmentEntity;
import org.activiti.engine.impl.persistence.entity.ByteArrayEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.engine.task.Attachment;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
// Not Serializable
public class CreateAttachmentCmd implements Command<Attachment> {  
  
  protected String attachmentType;
  protected String taskId;
  protected String processInstanceId;
  protected String attachmentName;
  protected String attachmentDescription;
  protected InputStream content;
  protected String url;
  
  public CreateAttachmentCmd(String attachmentType, String taskId, String processInstanceId, String attachmentName, String attachmentDescription, InputStream content, String url) {
    this.attachmentType = attachmentType;
    this.taskId = taskId;
    this.processInstanceId = processInstanceId;
    this.attachmentName = attachmentName;
    this.attachmentDescription = attachmentDescription;
    this.content = content;
    this.url = url;
  }
  
  public Attachment execute(CommandContext commandContext) {

    verifyParameters(commandContext);
    
    AttachmentEntity attachment = new AttachmentEntity();
    attachment.setName(attachmentName);
    attachment.setDescription(attachmentDescription);
    attachment.setType(attachmentType);
    attachment.setTaskId(taskId);
    attachment.setProcessInstanceId(processInstanceId);
    attachment.setUrl(url);
    
    DbSqlSession dbSqlSession = commandContext.getDbSqlSession();
    dbSqlSession.insert(attachment);
    
    if (content!=null) {
      byte[] bytes = IoUtil.readInputStream(content, attachmentName);
      ByteArrayEntity byteArray = new ByteArrayEntity(bytes);
      dbSqlSession.insert(byteArray);
      attachment.setContentId(byteArray.getId());
    }

    commandContext.getHistoryManager()
     .createAttachmentComment(taskId, processInstanceId, attachmentName, true);
    
    return attachment;
  }

  private void verifyParameters(CommandContext commandContext) {
    if (taskId != null) {
      TaskEntity task = Context.getCommandContext().getTaskManager().findTaskById(taskId);

      if (task == null) {
        throw new ActivitiException("Cannot find task with id " + taskId);
      }

      if (task.isSuspended()) {
        throw new ActivitiException("It is not allowed to add an attachment to a suspended task");
      }
    }
    
    if (processInstanceId != null) {
      ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(processInstanceId);

      if (execution == null) {
        throw new ActivitiException("Process instance " + processInstanceId + " doesn't exist");
      }

      if (execution.isSuspended()) {
        throw new ActivitiException("It is not allowed to add an attachment to a suspended process instance");
      }
    }
  }

}