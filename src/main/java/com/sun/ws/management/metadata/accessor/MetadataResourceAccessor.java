package com.sun.ws.management.metadata.accessor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.java.dev.wiseman.schemas.metadata.messagetypes.InputType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.MessageDefinitions;
import net.java.dev.wiseman.schemas.metadata.messagetypes.OperationNodeType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.OutputType;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xmlsoap.schemas.ws._2004._08.addressing.ReferenceParametersType;

import com.sun.ws.management.Management;
import com.sun.ws.management.ManagementUtility;
import com.sun.ws.management.ResourceStateDocument;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.enumeration.EnumerationMessageValues;
import com.sun.ws.management.enumeration.EnumerationUtility;
import com.sun.ws.management.eventing.Eventing;
import com.sun.ws.management.eventing.EventingMessageValues;
import com.sun.ws.management.eventing.EventingUtility;
import com.sun.ws.management.metadata.annotations.AnnotationProcessor;
import com.sun.ws.management.mex.Metadata;
import com.sun.ws.management.mex.MetadataUtility;
import com.sun.ws.management.transfer.Transfer;
import com.sun.ws.management.transport.HttpClient;


public class MetadataResourceAccessor extends JFrame {

	//Main method
	public static void main(String[] args) {
		new MetadataResourceAccessor();
	}
	//######## GUI Property/Fields List ############
	protected JTextField wisemanHost;
	private JMenuBar menuBar;
	private JButton submit;
	private JTextField metUid;
	private JTextField resFilter = null;
	private JTextField filteredResult = null;
	private JTextArea textArea=null;
	private JTextArea submittedContent = null;
	private JPanel msgPayloadPanel =null;
	private JTextArea returnedContent = null;
	private JCheckBox optimizedEnumeration = null;
	private JTextField eventSinkLocation;
	private JCheckBox isMetaDataUid;
	private JTextField transferMetadataId;
	private JCheckBox useDeletionId;
	private JTextField eventSourceLocation;
	private JCheckBox isEvtSrcMetaDataUid;
	private static MetadataResourceAccessor guiHandle;
	private static JComboBox loadedActions = null;
	private static ComboBoxModel model = null;
	private static JPanel soapActionsPanel=null;
    private static String EDIT = "Edit";
	private static String VIEW = "View";
    private static String BASE_AUTH = "wsman.basicauthentication";
	private static String BASE_USER = "wsman.user";
	private static String BASE_PASS = "wsman.password";

//	public static final String CRASH_ACTION_URI="http://com.hp.examples.eventing/ws/2007/06/Crash";
	
	// Constructor
	public MetadataResourceAccessor(){
		
		//frame settings
	    super("Metadata Resource Accessor");
	    setSize(600, 600);
	    setDefaultCloseOperation(EXIT_ON_CLOSE);
	    setLocationRelativeTo(null);

	    //Build service address panel and it's listener
	    JPanel servicePanel = new JPanel();
	     JLabel serviceAddress = new JLabel("Metadata Service Address/Host:");
	     //populate with good default data.
	     wisemanHost = new JTextField("http://<host>:<port>/<war name>/",20);
//	     wisemanHost = new JTextField("http://localhost:8080/dice/",20);
	     servicePanel.setLayout(new BoxLayout(servicePanel,BoxLayout.X_AXIS));
	     servicePanel.add(new JPanel());
	     servicePanel.add(serviceAddress); servicePanel.add(wisemanHost); 
	     servicePanel.add(new JPanel());
	     
	     //Build MetaDataUid set fields
	     JPanel metPanel = new JPanel();
	     JLabel metLabel = new JLabel("MetadataResourceUID:");
	     //populate with good default data.
	     metUid = new JTextField("(Insert valid metadataResourceId)",20);
//	     metUid = new JTextField(
//	    		 "http://localhost:8080/dice/SUBSCRIPTION-MANAGER-411",
//	    		 20);
	     metPanel.setLayout(new BoxLayout(metPanel,BoxLayout.X_AXIS));
	     metPanel.add(new JPanel());
	     metPanel.add(metLabel); metPanel.add(metUid);metPanel.add(new JPanel());
	     
	    //Attempt to load defined soapActions for the metadataResourceId
	    soapActionsPanel = new JPanel();
	     JButton loadActions = new JButton("Load Actions");
	       loadActions.addActionListener(new LoadActionsListener());
	     Object[] availableActions = {"(No Actions Loaded)"};
	     model = new DefaultComboBoxModel(availableActions);
	     loadedActions = new JComboBox(model);
	     soapActionsPanel.setLayout(new BoxLayout(soapActionsPanel,BoxLayout.X_AXIS));
	     soapActionsPanel.add(new JPanel());
	     soapActionsPanel.add(loadActions); 
	     soapActionsPanel.add(loadedActions); soapActionsPanel.add(new JPanel());
	    
	    //Build the operations panel 
	    JPanel opPanel = new JPanel();
	    //BUILD PANEL mesg specific elements to be added to the request
	    JPanel mesgAdditions = new JPanel();
	      mesgAdditions.setLayout(new BoxLayout(mesgAdditions,BoxLayout.Y_AXIS));
	      //Enumeration props
	      JLabel enumeration = new JLabel("Enumeration:");
	      optimizedEnumeration = new JCheckBox("Attempt Optimized Enumeration?");
	      JPanel row1 = new JPanel();row1.setLayout(new BoxLayout(row1,BoxLayout.X_AXIS));
	      row1.add(enumeration); row1.add(optimizedEnumeration); row1.add(new JPanel());
	      optimizedEnumeration.setSelected(true);
	      //Subscribe props
	      JLabel subscribe = new JLabel("Eventing:");
	      JLabel eventSink = new JLabel(" EventSink = ");
	      eventSinkLocation = new JTextField("(Enter EventSink Address)", 15);
	      isMetaDataUid = new JCheckBox("Is MetadataResourceUID?");
	      isMetaDataUid.setSelected(true);
	      JPanel row2 = new JPanel();row2.setLayout(new BoxLayout(row2,BoxLayout.X_AXIS));
	      row2.add(subscribe); row2.add(eventSink);
	      row2.add(eventSinkLocation);row2.add(isMetaDataUid);
	      //Subscribe EventSource
	      JLabel eventSrcLabel = new JLabel("Eventing:");
	      JLabel eventSource = new JLabel(" EventSource = ");
	      eventSourceLocation = new JTextField("(Enter EventSource Address)", 15);
	      isEvtSrcMetaDataUid = new JCheckBox("Is MetadataResourceUID?");
	      isEvtSrcMetaDataUid.setSelected(true);
	      JPanel row3 = new JPanel();row3.setLayout(new BoxLayout(row3,BoxLayout.X_AXIS));
	      row3.add(eventSrcLabel); row3.add(eventSource);
	      row3.add(eventSourceLocation);row3.add(isEvtSrcMetaDataUid);
	      //Deletion props
	      JLabel delete = new JLabel("Transfer:");
	      JLabel deleteMetId = new JLabel(" Identifier = ");
	       transferMetadataId = new JTextField("(Enter ResourcId for deletion)", 15);
	       useDeletionId = new JCheckBox("Use MetadataResourceUID?");
	       useDeletionId.setSelected(true);
	      JPanel row4 = new JPanel();row4.setLayout(new BoxLayout(row4,BoxLayout.X_AXIS));
	        row4.add(delete); row4.add(deleteMetId);
	        row4.add(transferMetadataId);row4.add(useDeletionId);
	     
        mesgAdditions.add(row1); mesgAdditions.add(row2); mesgAdditions.add(row3);
	    mesgAdditions.add(row4); 
	    
	    submit = new JButton("Submit Request");
	    submit.addActionListener(new SubmitActionListerner());
	     
	    opPanel.add(submit,BorderLayout.CENTER); 
	    opPanel.add(new JPanel()); 

	    menuBar = new JMenuBar();
	    JMenu formatMenu = new JMenu("Properties");
	    MenuAction editAction = new MenuAction(EDIT);
	    formatMenu.add(editAction);
	    MenuAction viewAction = new MenuAction(VIEW);
	    formatMenu.add(viewAction);
	    menuBar.add(formatMenu);
	    setJMenuBar(menuBar);
	    
	    //TOP PANEL
	    JPanel topPanel = new JPanel();
	    topPanel.setLayout(new BoxLayout(topPanel,BoxLayout.Y_AXIS));
	    topPanel.add(servicePanel); topPanel.add(metPanel); 
	    topPanel.add(soapActionsPanel); topPanel.add(opPanel);
	      JScrollPane scAdds = new JScrollPane(mesgAdditions);
	    topPanel.add(scAdds);
	    //BODY PANEL
	    JPanel bodyPanel = new JPanel();
	      JLabel outboundLabel = new JLabel("Outbound Content:");
	      JTabbedPane tabs = new JTabbedPane();
	      //Pure text panel
	        msgPayloadPanel = new JPanel();
	         textArea = new JTextArea("(Insert Payload Content)",10,35);
	          msgPayloadPanel.add(textArea,BorderLayout.CENTER);
	      //Submitted message panel
	        JPanel submittedPanel = new JPanel();
	      submittedContent = 
	    	  new JTextArea("(No submitted message to display)",10,35);
	      submittedPanel.add(submittedContent,BorderLayout.CENTER);  
	        
	      tabs.addTab("Message Request", submittedPanel);
	      tabs.addTab("XML Msg. Payload", msgPayloadPanel);
	      JScrollPane scp = new JScrollPane(tabs);
	      
	      JLabel returnedContentLabel = new JLabel("Message Response:");
	      returnedContent = new JTextArea("(No payload to display)",10,35);
	       JPanel filterPanel = new JPanel();
	        JLabel resourceStateLabel = new JLabel("ResourceState Filter:");
	        resFilter = new JTextField("//*[local-name()='age']",15);
	         filterPanel.add(resourceStateLabel); filterPanel.add(resFilter);
	        JLabel filtResultLabel = new JLabel(" = ");
	        filteredResult = new JTextField("(No results to display)",15);
	        filteredResult.setEditable(false);
	        filterPanel.add(filtResultLabel);filterPanel.add(filteredResult);
	     //populate bodyPanel
	     bodyPanel.setLayout(new BoxLayout(bodyPanel,BoxLayout.Y_AXIS));
	     bodyPanel.add(outboundLabel);
	     bodyPanel.add(scp);
	     bodyPanel.add(returnedContentLabel);
	      JScrollPane scContent = new JScrollPane(returnedContent);
	     bodyPanel.add(scContent);
	     bodyPanel.add(filterPanel);
	      
	    getContentPane().add(topPanel, BorderLayout.NORTH);
	    getContentPane().add(bodyPanel, BorderLayout.CENTER);
	    setVisible(true);
	    guiHandle = this;
	  }
	  
	//Construct the properties menus
	  class MenuAction extends AbstractAction {
		    public MenuAction(String text) {
		      super(text);
		    }
		    public void actionPerformed(ActionEvent e) {
		    	String message="";
		    	if(e.getActionCommand().equals(EDIT)){
		    		message+="Enter new name value properties \n";
		    		message+="to be set for the Viewer. \n";
		    		message+="Ex. "+BASE_AUTH+"=true, \n";
		    		message+=BASE_USER+"=wsman, "+BASE_PASS+"=secret \n";
		    		String response = JOptionPane.showInputDialog(guiHandle, 
		    				message, 
		    				"Add/Edit parameters...", 
		    				JOptionPane.PLAIN_MESSAGE);
		    		//process response to overwrite the values passed in
		    		if((response!=null)&&(response.trim().length()>0)){
		    			StringTokenizer tokens = new StringTokenizer(response," ,");
		    			while(tokens.hasMoreElements()){
		    			   StringTokenizer nvp =
		    				   new StringTokenizer(tokens.nextToken(),"=");
		    			   if(nvp.countTokens()==2){
		    				  String token = nvp.nextToken(); 
		    				  if(token.equals(BASE_AUTH)){
		    					   System.setProperty(BASE_AUTH, nvp.nextToken());
		    				  }
		    				  if(token.equals(BASE_USER)){
		    					   System.setProperty(BASE_USER, nvp.nextToken());
		    				  }
		    				  if(token.equals(BASE_PASS)){
		    					  System.setProperty(BASE_PASS, nvp.nextToken());
		    				  }
		    			   }
		    			}
		    		}
		    	}
		    	if(e.getActionCommand().equals(VIEW)){
		    		message = "";
		    		String authEnabled = BASE_AUTH;
		    		String u = BASE_USER;
		    		String p = BASE_PASS;
		    		message+=authEnabled+"="+System.getProperty(authEnabled)+"\n";
		    		message+=u+"="+System.getProperty(u)+"\n";
		    		message+=p+"="+System.getProperty(p)+"\n";
		        	JOptionPane.showMessageDialog(
		        			guiHandle, 
		        			message, 
		        			"Viewing current property values...", 
		        			JOptionPane.PLAIN_MESSAGE);
		    	}
		    }
	  }
	  
	  private static void initializeAuthenticationData(){
	    //Add authentication mechanism
		final String basicAuth = System.getProperty(BASE_AUTH);
        if ("true".equalsIgnoreCase(basicAuth)) {
            HttpClient.setAuthenticator(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    final String user = System.getProperty(BASE_USER, "");
                    final String password = System.getProperty(BASE_PASS, "");
                    return new PasswordAuthentication(user, password.toCharArray());
                }
            }
            );
        }else{//Display message to the effect that connection parameters were not located
        	String message="Unable to locate the system propety \n"+
        		"'"+BASE_AUTH+"'. Please set the following parameters: \n"+
        		"'"+BASE_AUTH+"', '"+BASE_USER+"' and '"+BASE_PASS+"' \n"	+
        		" in the drop down menu or pass them in as -D parameters.\n" ;
        	   message+="This tool may not perform without those values.\n";
        	JOptionPane.showMessageDialog(guiHandle, message, 
        			"Failure to locate http credentials...", 
        			JOptionPane.WARNING_MESSAGE);
        }
	  }
	  
//	 public static String xmlToString(Node node) {
//		try {
//			Source source = new DOMSource(node);
//			StringWriter stringWriter = new StringWriter();
//			Result result = new StreamResult(stringWriter);
//			TransformerFactory factory = TransformerFactory.newInstance();
//			Transformer transformer = factory.newTransformer();
//			transformer.transform(source, result);
//			return stringWriter.getBuffer().toString();
//		} catch (TransformerConfigurationException e) {
//			e.printStackTrace();
//		} catch (TransformerException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}

	 private class LoadActionsListener implements ActionListener{

		public void actionPerformed(ActionEvent e) {
   		 initializeAuthenticationData();
			//extract common service metadata
			String metaDataUID = metUid.getText();
			String serviceAddress = wisemanHost.getText();
			
			String[] actionList = null;
			actionList = populateActions(metaDataUID, serviceAddress);
			model = new DefaultComboBoxModel(actionList);
			
			loadedActions.setModel( model );
		}
	 }

    private String[] populateActions(String metaDataUID, String serviceAddress){
    	String[] actionList = null;
		Management meta = null;
		try {
			QName[] trimHeaders = null;
			meta = AnnotationProcessor.findAnnotatedResourceByUID(
					metaDataUID, serviceAddress,false,trimHeaders);
		} catch (Exception e1) {
			e1.printStackTrace();
			String msg="There was an error retrieving information from the metadata server,\n";
			msg+="'"+serviceAddress+"' when using the metadataResourceUID '";
			msg+=metaDataUID+"'.\n";
			msg+=e1.getMessage();
			throw new RuntimeException(msg);
		} 
		if((meta!=null)&&(meta.getBody().getFirstChild()!=null)){
		   Document mesgDefinition;
		try {
		   mesgDefinition = meta.getBody().extractContentAsDocument();
		   MessageDefinitions returnedMessage = null; 
		   Object unmarshalled = meta.getXmlBinding().unmarshal(mesgDefinition);
		   if(unmarshalled instanceof MessageDefinitions){
			   returnedMessage = (MessageDefinitions) unmarshalled;
		   }
		   //Navigate down into the message def to locate actions
		   if(returnedMessage!=null){
			  if(returnedMessage.getOperations()!=null){
				 if(returnedMessage.getOperations().getOperation()!=null){
				   List<OperationNodeType> nodes = 
					   returnedMessage.getOperations().getOperation();
				   ArrayList<String> locatedSoapActions = new ArrayList<String>();
				   for(OperationNodeType op: nodes){
					 String actValue = null;					   
				     InputType input = op.getInput();
				     //locate the input soapAction if defined
				     if((input!=null)&&(input.getAction()!=null)){
				      	actValue = input.getAction();
				      	if(actValue.trim().length()>0){
				      	 locatedSoapActions.add(actValue.trim());
				      	}
				     }
					 //now put all of the loaded actions into the combobox
				     if(locatedSoapActions.size()>0){
				      actionList =new String[locatedSoapActions.size()];
				      actionList = locatedSoapActions.toArray(actionList);
				      final String[] list = actionList;	 
				     }
				   
				 }//end of for loop
			  }//end of if operation not null
		   }//end of ops check
		}//end of check for content to display
		}
		catch (SOAPException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JAXBException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	   }
	  return actionList;
    }
    
    private class SubmitActionListerner implements ActionListener{
    	 public void actionPerformed(ActionEvent e) {
    		 initializeAuthenticationData();
    		 //locate button selected
    	   if(loadedActions.getSelectedItem()==null){
    		  String msg="No action was selected. Unable to proceed.";
    		  throw new RuntimeException(msg);
    	   }
    		 
    		 final String action = (String) loadedActions.getSelectedItem();
    	   try{
    		 //extract the outbound content
			   String metaDataUID = metUid.getText();
			   String serviceAddress = wisemanHost.getText();
			   Management message;
			   
			   message = AnnotationProcessor.findAnnotatedResourceByUID(
					   metaDataUID,serviceAddress);
			    //GET_ACTION
				if(action.equals(Transfer.GET_ACTION_URI)){//Get action
					Management get = message;
					//set Action to request of the service
					get.setAction(Transfer.GET_ACTION_URI);
					//run this Management instance through ManagementUtility
					get = ManagementUtility.buildMessage(get, null);
					//Use the default HttpClient or your own here to send the message off
					System.out.println("MessageBeingSubmit:"+get);
					submittedContent.setText(get.toString());
					Addressing response =HttpClient.sendRequest(get);
					
					ResourceStateDocument resState = ManagementUtility.getAsResourceState(response);
					String content = response.toString();
					
					returnedContent.setText(content);
					String xpath = resFilter.getText();
					if((xpath!=null)&&(xpath.trim().length()>0)){
						String result = resState.getValueText(xpath);
						if((result!=null)&&(result.trim().length()>0)){
							filteredResult.setText(result); 
						}
					}
				} //DELETE ACTION processing
				else if(action.equals(Transfer.DELETE_ACTION_URI)){//Delete action
    			   Management get = message;
    			   //set Action to request of the service
    			   get.setAction(Transfer.DELETE_ACTION_URI);
    			   //TODO: add and additional header to define which resource to delete
    			   if(isMetaDataUid.isSelected()){
    				  String id = transferMetadataId.getText();
    				  ReferenceParametersType refParType = 
    					  Management.createReferenceParametersType(
    							  EventingMessageValues.EVENT_SINK, id);
    				  get.addHeaders(refParType);
    			   }
    			   //run this Management instance through ManagementUtility
    			   get = ManagementUtility.buildMessage(get, null);
    			   //Use the default HttpClient or your own here to send the message off
				   submittedContent.setText(get.toString());
    			   Addressing response =HttpClient.sendRequest(get);
    			   
    			   ResourceStateDocument resState = ManagementUtility.getAsResourceState(response);
    			   String content = response.toString();
    			   
    			   returnedContent.setText(content);
    			   String xpath = resFilter.getText();
    			   if((xpath!=null)&&(xpath.trim().length()>0)){
    				   String result = resState.getValueText(xpath);
    				   if((result!=null)&&(result.trim().length()>0)){
    					   filteredResult.setText(result); 
    				   }
    			   }
    		   } //SUBSCRIBE ACTION processing
    		   else if(action.equals(Eventing.SUBSCRIBE_ACTION_URI)){
    			   Eventing subscribe = new Eventing(message);
    			   //Set the action
    			   subscribe.setAction(Eventing.SUBSCRIBE_ACTION_URI);
    			   EventingMessageValues evtMesSettings = new EventingMessageValues();
    			   	  evtMesSettings.setEventSinkDestination(eventSinkLocation.getText());
    			   	  if(isMetaDataUid.isSelected()){
    			   		ReferenceParametersType refMetUid = 
    			   			Management.createReferenceParametersType(
    			   			 AnnotationProcessor.RESOURCE_META_DATA_UID, eventSinkLocation.getText()); 
    			   		 evtMesSettings.setEventSinkReferenceParameterType(refMetUid);
    			   		 //set the notification address
    			   	  }else{
    			   		 //set the addressing to part for the EPR
    			   		 evtMesSettings.setEventSinkDestination(eventSinkLocation.getText());
    			   	  }
    			   //run this Management instance through ManagementUtility
    			   subscribe = EventingUtility.buildMessage(new Eventing(subscribe), 
    					   evtMesSettings);
    			   //Use the default HttpClient or your own here to send the message off
				   submittedContent.setText(subscribe.toString());
    			   Addressing response =HttpClient.sendRequest(subscribe);
    			   
    			   ResourceStateDocument resState = ManagementUtility.getAsResourceState(response);
    			   String content = response.toString();
    			   
    			   returnedContent.setText(content);
    			   String xpath = resFilter.getText();
    			   if((xpath!=null)&&(xpath.trim().length()>0)){
    				   String result = resState.getValueText(xpath);
    				   if((result!=null)&&(result.trim().length()>0)){
    					   filteredResult.setText(result); 
    				   }
    			   }
    		   } //INITIALIZE processing ... 
    		   else if(action.equals(Metadata.INITIALIZE_ACTION_URI)){
    			   Management subscribe = message;
    			   //Set the action
    			   subscribe.setAction(Metadata.INITIALIZE_ACTION_URI);
    			   //run this Management instance through ManagementUtility
    			   subscribe = ManagementUtility.buildMessage(subscribe, null);
    			   //Use the default HttpClient or your own here to send the message off
				   submittedContent.setText(subscribe.toString());	
    			   Addressing response =HttpClient.sendRequest(subscribe);

    			   ResourceStateDocument resState = ManagementUtility.getAsResourceState(response);
    			   String content = response.toString();
    			   
    			   returnedContent.setText(content);
    			   String xpath = resFilter.getText();
    			   if((xpath!=null)&&(xpath.trim().length()>0)){
    				   String result = resState.getValueText(xpath);
    				   if((result!=null)&&(result.trim().length()>0)){
    					   filteredResult.setText(result); 
    				   }
    			   }
    		   }
    		   else if(action.equals(Enumeration.ENUMERATE_ACTION_URI)){//Enumeration action
    			Management enumeration = message; 
		    	//set Action to request of the service
			    enumeration.setAction(Enumeration.ENUMERATE_ACTION_URI);
			    EnumerationMessageValues enMess = EnumerationMessageValues.newInstance();
			    if(optimizedEnumeration.isSelected()){
			    	enMess.setRequestForOptimizedEnumeration(true);	
			    }
			    enumeration = new Management(
			    	EnumerationUtility.buildMessage(
			    	new Enumeration(enumeration), enMess));
		    	//run this Management instance through ManagementUtility
			    enumeration = ManagementUtility.buildMessage(enumeration, null);
				submittedContent.setText(enumeration.toString());
		    	//Use the default HttpClient or your own here to send the message off
		    	Addressing response =HttpClient.sendRequest(enumeration);
		    	
		    	ResourceStateDocument resState = ManagementUtility.getAsResourceState(response);
		    	String content = response.toString();
		    	
		    	returnedContent.setText(content);
		    	String xpath = resFilter.getText();
		    	if((xpath!=null)&&(xpath.trim().length()>0)){
		    	  String result = resState.getValueText(xpath);
		    	  if((result!=null)&&(result.trim().length()>0)){
		    		 filteredResult.setText(result); 
		    	  }
		    	}
    		 }//CREATE_ACTION
   		     else if(action.equals(Transfer.CREATE_ACTION_URI)){
			   Management transfer = message;
			   //Set the action
			   transfer.setAction(Transfer.CREATE_ACTION_URI);
			   //run this Management instance through ManagementUtility
			   transfer = ManagementUtility.buildMessage(transfer, null);
			   //Check to see if body should be added from the 
			   
			   //Use the default HttpClient or your own here to send the message off
			   submittedContent.setText(transfer.toString());	
			   Addressing response =HttpClient.sendRequest(transfer);
			   
			   ResourceStateDocument resState = ManagementUtility.getAsResourceState(response);
			   String content = response.toString();
			   
			   returnedContent.setText(content);
   		     }//UNSUBSCRIBE
    		 else if(action.equals(Eventing.UNSUBSCRIBE_ACTION_URI)){
  			   Management eventing = message;
			   //Set the action
  			   eventing.setAction(Eventing.UNSUBSCRIBE_ACTION_URI);
			   //run this Management instance through ManagementUtility
  			   eventing = ManagementUtility.buildMessage(eventing, null);
  			   //Add metadata indicating which eventsink,eventsource
  			   ReferenceParametersType evtSink = Management.createReferenceParametersType(
  					   EventingMessageValues.EVENT_SINK, 
  					   eventSinkLocation.getText());
  			   eventing.addHeaders(evtSink);
  			   ReferenceParametersType evtSrc = Management.createReferenceParametersType(
  					   EventingMessageValues.EVENTING_COMMUNICATION_CONTEXT_ID, 
  					   eventSourceLocation.getText());
  			   eventing.addHeaders(evtSrc);
  			   
  			   //Check to see if body should be added from the 
			   //Use the default HttpClient or your own here to send the message off
			   submittedContent.setText(eventing.toString());	
			   Addressing response =HttpClient.sendRequest(eventing);

			   ResourceStateDocument resState = ManagementUtility.getAsResourceState(response);
			   String content = response.toString();
			   
			   returnedContent.setText(content);

    		 }
    		 else{//
    			String msg="This tool does not know how to build a valid message for:";
    			msg+="'"+action+"'. Please select another action.";
    			throw new RuntimeException(msg);
    		 }
			} catch (Exception e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(guiHandle, e1.getMessage(), 
						"Operations error:", JOptionPane.ERROR_MESSAGE);
			} 
    	 }
    }
}
