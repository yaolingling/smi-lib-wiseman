/*
 * Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 ** Copyright (C) 2006, 2007 Hewlett-Packard Development Company, L.P.
 **  
 ** Authors: Simeon Pinder (simeon.pinder@hp.com), Denis Rachal (denis.rachal@hp.com), 
 ** Nancy Beers (nancy.beers@hp.com), William Reichardt 
 **
 **$Log: MetadataViewer.java,v $
 **Revision 1.5  2007/06/22 06:13:56  simeonpinder
 **numerous changes made for final build:
 **-release properties update
 **-added progress monitor to Metadata viewer
 **-added commented code for fragment operations with traffic light
 **-added extra example line for cmdline tool
 **-license files for 1.0 release
 **-updated .classpath files for all samples
 **-renamed eventing sample name.
 **-small release note mention in README
 **
 **Revision 1.4  2007/06/19 12:29:35  simeonpinder
 **changes:
 **-set 1.0 release implementation version
 **-enable metadata ResourceURIs from extracted EPR
 **-useful eventing constants and fix for notifyTo in utility.
 **-cleaned up EventSourceInterface,SubscriptionManagerInterface definitions
 **-added MetadataResourceAccessor draft
 **-improved mechanism to strip unwanted headers from metadata decorated Management mesgs
 **-added unregister mechanism to facilitate remote SubscriptionManager implementations
 **
 **Revision 1.3  2007/05/30 20:30:32  nbeers
 **Add HP copyright header
 **
 ** 
 *
 * $Id: MetadataViewer.java,v 1.5 2007/06/22 06:13:56 simeonpinder Exp $
 */
package com.sun.ws.management.metadata.viewer;

import com.sun.ws.management.mex.MetadataUtility;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringWriter;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
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
import net.java.dev.wiseman.schemas.metadata.messagetypes.OperationsType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.OutputType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.SchemaType;
import net.java.dev.wiseman.schemas.metadata.messagetypes.SchemasType;

import org.dmtf.schemas.wbem.wsman._1.wsman.SelectorType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.ws.management.Management;
import com.sun.ws.management.ManagementUtility;
import com.sun.ws.management.addressing.Addressing;
import com.sun.ws.management.identify.Identify;
import com.sun.ws.management.identify.IdentifyUtility;
import com.sun.ws.management.metadata.annotations.AnnotationProcessor;
import com.sun.ws.management.transfer.Transfer;
import com.sun.ws.management.transfer.TransferUtility;
import com.sun.ws.management.transport.HttpClient;
import com.sun.ws.management.xml.XmlBinding;

public class MetadataViewer extends JFrame {
	
	  //default fields
	  private static final Logger LOG = Logger.getLogger(MetadataViewer.class.getName());
	  private JScrollPane scrollpane;
	  private JTree defaultTree;
	  private DefaultTreeModel model = null;
	  private static JTextArea messages = null;
	  private DefaultMutableTreeNode rootNode = null;
	  private JButton load = null;
	  private boolean completed=false;
	  private JTextField wisemanHost = null;
	  private static MetadataViewer guiHandle = null;
	  static DefaultMutableTreeNode emptyNode = null;
	  public JMenuBar menuBar;
	  private static String EDIT = "Edit";
	  private static String VIEW = "View";
	  private static String BASE_AUTH = "wsman.basicauthentication";
	  private static String BASE_USER = "wsman.user";
	  private static String BASE_PASS = "wsman.password";
	  private static 
	  net.java.dev.wiseman.schemas.metadata.messagetypes.ObjectFactory 
		metadataContent_fact = new 
		net.java.dev.wiseman.schemas.metadata.messagetypes.ObjectFactory();
	  private static XmlBinding binding = null;
	  
	  public MetadataViewer() {
		//frame settings
	    super("Metadata Viewer");
	    setSize(400, 300);
	    setDefaultCloseOperation(EXIT_ON_CLOSE);
	    setLocationRelativeTo(null);

	    //root element settings
	    rootNode = new DefaultMutableTreeNode("Exposed Wiseman Metadata Resources");
	    emptyNode = 
	    	new DefaultMutableTreeNode("(Currently no metadata to display, 'Load' to begin.)");
	    rootNode.add(emptyNode);
	    model = new DefaultTreeModel(rootNode);
	    defaultTree = new JTree(model);
	    defaultTree.setShowsRootHandles(true);
	    defaultTree.setEditable(false);
	    
	    //Build button panel and it's listener
	    JPanel topPanel = new JPanel();
	    load = new JButton("Load");
	    ActionListener butLis = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				initializeAuthenticationData();
				completed = false;
				//locate button contents
				final String host = wisemanHost.getText().trim();
				//add a progress bar.
				final long sleep = 500;
				UIManager.put("ProgressMonitor.progressText", "Loading");
				final ProgressMonitor progressMonitor = 
					new ProgressMonitor(guiHandle,
                        "Loading metadata from server...",
                        null, 50, 100);
				progressMonitor.setMillisToDecideToPopup(0);
				progressMonitor.setMillisToPopup(0);
				//Run the work in thread to avoid locking up UI thread
				new Thread(){
					public void run() {
						MetadataViewer.loadHostMetaData(host,model);
						completed = true;
					}
				}.start();
				//Run the work in thread to avoid locking up UI thread
				new Thread(){
				  public void run() {
					  int inc = 50; int max = 500*2*60*2;//2 mins...
					  while((!completed)&&(inc<max)){	
						  progressMonitor.setProgress(inc++);
						try {
							Thread.sleep(sleep);
						} catch (InterruptedException e) {
						}
					  }
					  progressMonitor.close();
					}
				}.start();
//				MetadataViewer.loadHostMetaData(host,model);  
			}
	    };
	    load.addActionListener(butLis);
	    //populate with good default data.
//	    wisemanHost = new JTextField("http://localhost:8080/wsman/",20);
	    wisemanHost = new JTextField("http://<host>:<port>/<webapp name>/",20);
	    topPanel.add(load); topPanel.add(wisemanHost);
	    scrollpane = new JScrollPane(defaultTree);
	    menuBar = new JMenuBar();
	    JMenu formatMenu = new JMenu("Properties");
	    MenuAction editAction = new MenuAction(EDIT);
	    formatMenu.add(editAction);
	    MenuAction viewAction = new MenuAction(VIEW);
	    formatMenu.add(viewAction);
	    menuBar.add(formatMenu);
	    setJMenuBar(menuBar);
//	    //add a logging text area
//	    messages=new JTextArea("Logging and message exchange may be shown in this area)");
//	    JScrollPane mesPane = new JScrollPane(messages);
//	    JPanel messagePanel = new JPanel();
//	    messagePanel.add(mesPane);
	    
	    getContentPane().add(scrollpane, BorderLayout.CENTER);
	    getContentPane().add(topPanel, BorderLayout.NORTH);
//	    getContentPane().add(messagePanel, BorderLayout.SOUTH);
	    setVisible(true);
	    guiHandle = this;
	  }
	  
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
	  
	  private static void loadHostMetaData(String host, DefaultTreeModel rootTreeModel){
		  
		  DefaultMutableTreeNode rootNodeElement = (DefaultMutableTreeNode) rootTreeModel.getRoot();
		  if((host==null)||(host.trim().length()==0)){
			  return;
		  }
	      //post pend the trailing forward slash if it's not provided
		  if(!host.trim().endsWith("/")){
			 host = host+"/"; 
		  }
		  try {
	    	//############ REQUEST THE METADATA REPOSITORY DATA ######################
	        //Request identify info to get MetaData root information
	        final Identify identify = new Identify();
	        identify.setIdentify();
	        
	        //Send identify request
	        final Addressing response = HttpClient.sendRequest(identify.getMessage(), host);
	        if (response.getBody().hasFault()) {
	        	LOG.fine(response.getBody().getFault().getFaultString());
	        	return;
	        }
	        
	        //Parse the identify response
	        final Identify id = new Identify(response);
	        SOAPElement el =IdentifyUtility.locateElement(id, 
	        		AnnotationProcessor.META_DATA_RESOURCE_URI); 
	         //retrieve the MetaData ResourceURI
	         String resUri=el.getTextContent();
	         el =IdentifyUtility.locateElement(id, 
	        		AnnotationProcessor.META_DATA_TO);
	        //retrieve the MetaData To/Destination
	        String metTo=el.getTextContent();

	        //############ REQUEST THE LIST OF METADATA AVAILABLE ######################
		   //Build the GET request to be submitted for the metadata
	        Management m = null; 
	        m =TransferUtility.createMessage(metTo, resUri,
	        		Transfer.GET_ACTION_URI, null, null, 30000, null);
	         //############ PROCESS THE METADATA RESPONSE ######################
	         //Parse the getResponse for the MetaData
	         final Addressing getResponse = HttpClient.sendRequest(m);
	       Management mResp = new Management(getResponse);
	       //Determine if any Metadata to display
		    //########### TRANSLATE METADATA TO FAMILIAR MANAGEMENT NODES ##### 
		     //Extract the MetaData node returned as Management instances
		     Management[] metaDataList = 
		        	MetadataUtility.extractEmbeddedMetaDataElements(mResp);
		     if((metaDataList!=null)&&(metaDataList.length>0)){
		    	 //remove (no metadata to display node)
		    	 if(rootNodeElement.getIndex(emptyNode)>-1){
		    		 rootNodeElement.remove(emptyNode);
		    	 }
		    	 
		    	 // update the jtree and it's sub components.
		    	 DefaultMutableTreeNode hostRoot = new DefaultMutableTreeNode(host);
		    	 for (int i = 0; i < metaDataList.length; i++) {
		    		 Management man = metaDataList[i];
		    		 DefaultMutableTreeNode node = null;
		    		 SOAPElement value = 
		    			 ManagementUtility.locateHeader(man.getHeaders(), 
		    					 AnnotationProcessor.RESOURCE_META_DATA_UID);
		    		 supportsEnumerations(man);
		    		if((value==null)||(value.getTextContent()==null)||
		    				(value.getTextContent().trim().length()==0)){
		    		  node = new DefaultMutableTreeNode(
		    				  "(No Unique identifier defined for this node. Please add one.)");
		    		}else{
		    		  if(supportsEnumerations(man)){
		    		   node = new DefaultMutableTreeNode(
		    				   value.getTextContent()+" -(suppports Enumeration)");  
		    		  }else{
					   node = new DefaultMutableTreeNode(value.getTextContent());
		    		  }
		    		}
		    		//now put all the sub elements of the metadata as leaves below this node
		    		populateNodeWithMetadata(node,man);

		    		//add the node to the root.
					hostRoot.add(node);
				 }
		    	//prune for duplicates. 
		        Enumeration peers = rootNodeElement.children();
		        while (peers.hasMoreElements()) {
					DefaultMutableTreeNode element = 
						(DefaultMutableTreeNode) peers.nextElement();
					if(element.toString().equals(hostRoot.toString())){
						rootNodeElement.remove(element);
					}
				}

		        //put in the host root node
		    	rootNodeElement.add(hostRoot);
		    	rootTreeModel.reload(rootNodeElement);
		     }
			}catch(Exception e){
			   String message = e.getMessage();
			   for (int i = 0; i < e.getStackTrace().length; i++) {
				   message+="\n"+e.getStackTrace()[i].toString();
			   }
			   JOptionPane.showMessageDialog(guiHandle, 
					   message, 
					   "Error occurred during processing...", 
					   JOptionPane.ERROR_MESSAGE);
				System.out.println("Exception occurred:"+message);
			}
	  }

	/** Tests whether the management instance passed in contains any enumeration
	 * headers. 
	 * @param man
	 * @throws SOAPException
	 */
	private static boolean supportsEnumerations(Management man) throws SOAPException {
		boolean enumerationEnabled;
		 SOAPElement enumValue = 
			 ManagementUtility.locateHeader(man.getHeaders(), 
					 AnnotationProcessor.ENUMERATION_ACCESS_RECIPE);
		  SOAPElement enumValue2 = 
			ManagementUtility.locateHeader(man.getHeaders(), 
					AnnotationProcessor.ENUMERATION_FILTER_USAGE);
		  enumerationEnabled= false;
		  if(enumValue!=null){
			enumerationEnabled=true; 
		  }
		  if(enumValue2!=null){
			enumerationEnabled=true; 
		  }
		return enumerationEnabled;
	}
	  
	  /** Take a root node and a Management instance and populates a text
	   * representation of the contents of the Management node as child 
	   * elements of the node passed in.
	   * 
	   * @param node DefaultMutableTreeNode instance, should be root of project.
	   * @param man Management instance with Management addressing information.
	   * @throws JAXBException
	   * @throws SOAPException
	   */
	  private static void populateNodeWithMetadata(DefaultMutableTreeNode node, 
			  Management man) throws JAXBException, SOAPException {
		 if((node ==null)||(man==null)){
			 return;
		 }
		 else{
		   SOAPElement[] allHeaders = man.getHeaders();
		   QName selQName = Management.SELECTOR_SET;
		   for (int i = 0; i < allHeaders.length; i++) {
			   SOAPElement header = allHeaders[i];
			   //special parsing for SelectorSet.
			  if(selQName.getLocalPart().equals(header.getElementQName().getLocalPart())&&
				 selQName.getNamespaceURI().equals(header.getElementQName().getNamespaceURI())){
				Map<String, String> selectorsRetrieved = ManagementUtility.extractSelectorsAsMap(null,
					(List)new ArrayList<SelectorType>(man.getSelectors()));
				addNodeForElement(node, selectorsRetrieved); 
			  }
			  else{
				addNodeForElement(node, header);
			  }
		   }//End of SoapEnvelope processing
		   //Now process for MessageDefinition details
		   if(man.getBody().getFirstChild()!=null){
			   if(binding==null){binding = man.getXmlBinding();}
			   //Create JAXB type for the embedded node
			   Document mesgDefinition = man.getBody().extractContentAsDocument();
			   MessageDefinitions returnedMessage = null; 
			   Object unmarshalled = binding.unmarshal(mesgDefinition);
			   if(unmarshalled instanceof MessageDefinitions){
				   returnedMessage = (MessageDefinitions) unmarshalled;
			   }
			  if(returnedMessage!=null){
			     //Then add content for root meta-data definition
					DefaultMutableTreeNode metDefNode = 
						new DefaultMutableTreeNode("message-definitions");	
		        //Then add child content for schemas
				SchemasType schemas = returnedMessage.getSchemas();
				DefaultMutableTreeNode schemNode = 
					new DefaultMutableTreeNode("schemas[prefix=schemaLocation]-(accessible by HTTP)");
				for(SchemaType schema: schemas.getSchema()){
					addNodeForSchemaType(schemNode, schema);
				}
			    //Then add child content for OperationsType
				OperationsType opers = returnedMessage.getOperations();
				DefaultMutableTreeNode operNode = 
					new DefaultMutableTreeNode("operations");
				for(OperationNodeType operation: opers.getOperation()){
					addNodeForOperationNodeType(operNode, operation);
				}
				
				//stitch it all together
				metDefNode.add(schemNode);
				metDefNode.add(operNode);
				node.add(metDefNode);
				
			  }//end of IF messageDefinitionType successfully created
		   }
		 }
	  }
	
	/** Adds the selector content as a displayable leaf node of the node passed in.
	 * 
	 * @param node
	 * @param selectorsRetrieved
	 */  
	private static void addNodeForElement(DefaultMutableTreeNode node, Map<String, String> selectorsRetrieved) {
		DefaultMutableTreeNode newNode,sel;
		if(selectorsRetrieved!=null){
			newNode = new DefaultMutableTreeNode(Management.SELECTOR_SET.getLocalPart());
			for (Iterator iter = selectorsRetrieved.keySet().iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				sel = new DefaultMutableTreeNode(element+"="+selectorsRetrieved.get(element));
				newNode.add(sel);
			}
			node.add(newNode);
		}
	}

	/** Adds the SoapElement contents as a displayable leaf node of the node passed in. 
	 * @param node 
	 * @param locatedElement
	 */
	private static void addNodeForElement(DefaultMutableTreeNode node, SOAPElement locatedElement) {
		DefaultMutableTreeNode newNode;
		if(locatedElement!=null){
			newNode = new DefaultMutableTreeNode(
					locatedElement.getElementQName().getLocalPart()+"="+
			locatedElement.getTextContent());
			node.add(newNode);
		}
	}

	/** Adds the SchemaType's contents as a displayable leaf node of the node passed in. 
	 * @param node 
	 * @param schema
	 */
	private static void addNodeForSchemaType(DefaultMutableTreeNode node, 
			SchemaType schema) {
		DefaultMutableTreeNode newNode;
		if(schema!=null){
			newNode = new DefaultMutableTreeNode(
					schema.getPrefix()+"="+
					schema.getValue());
			node.add(newNode);
		}
	}

	/** Adds the OperationNodeType's contents as a displayable leaf node of 
	 * the node passed in. 
	 * @param node 
	 * @param operation
	 */
	private static void addNodeForOperationNodeType(DefaultMutableTreeNode node, 
			OperationNodeType operation) {
		DefaultMutableTreeNode newNode;
		if(operation!=null){
			newNode = new DefaultMutableTreeNode(
					operation.getName());
		  //BUILD the input node
		  String message="(Schema Type for Soap Action content)";	
		  InputType input = operation.getInput();
		  DefaultMutableTreeNode inpNode = 
			  new DefaultMutableTreeNode("input"+message);
		  DefaultMutableTreeNode inpMsgType = 
			  new DefaultMutableTreeNode("xsdType="+
				input.getMessage());
		  DefaultMutableTreeNode inpSoapAction = 
			  new DefaultMutableTreeNode("soapAction="+
				input.getAction());
		    inpNode.add(inpMsgType);
		    inpNode.add(inpSoapAction);
		  //BUILD the output node  
		  OutputType output = operation.getOutput();
		  DefaultMutableTreeNode outpNode = 
			  new DefaultMutableTreeNode("output"+message);
		  DefaultMutableTreeNode outpMsgType = 
			  new DefaultMutableTreeNode("msgType="+
				output.getMessage());
		  DefaultMutableTreeNode outpSoapAction = 
			  new DefaultMutableTreeNode("soapAction="+
				output.getAction());
		    outpNode.add(outpMsgType);
		    outpNode.add(outpSoapAction);
		  
		  //Stitch it all together
	      newNode.add(inpNode);  
		  newNode.add(outpNode);  
		    
		  //Add all the new nodes to the root node
		  node.add(newNode);
		}
	}
	
	/**To enable launch from command line.
	 * @param args 
	 */
	public static void main(String args[]) {
	    new MetadataViewer();
    }

}
