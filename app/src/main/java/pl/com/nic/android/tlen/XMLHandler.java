package pl.com.nic.android.tlen;

import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


/* requires 2.2 */
/*
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.dom.DOMSource;
 */

class 
XMLHandler extends DefaultHandler
{
	private String TAG = "XMLHandler";

	DocumentBuilderFactory dFactory;
	DocumentBuilder        dBuilder;
	DOMImplementation      dImpl;
	Document               doc;
	Element                root;
	Handler                ntHandler;

	Context ctx;

	String sessionId;

	private String
	getTextContent(Element node)
	{
		if (node == null) {
			return "";
		}

		/* requires 2.2 */
		// return node.getTextContent();

		Node child = (Node) node.getFirstChild();
		if (child == null)
			return "";
		else
			return child.getNodeValue();
	}


	public void
	setTextContent(Element element, String data)
	{
		/* reqires 2.2 */
		//element.setTextContent(data);

		Text t = doc.createTextNode(data);
		root.appendChild(t);
	}


	public void
	postToNT(String cmd, Bundle b)
	{
		Log.d(TAG, "sending to ts: " + cmd);
		
		CommonUtils.postToTlenService(ctx, cmd, b);
	}
	
	
	public void
	postToNT(String cmd)
	{
		Bundle b = new Bundle();
		
		postToNT(cmd, b);
	}


	public void
	process_avatar_token(Element xml)
	{
		/* <avatar><token>RLTMCXfryTZfGDojqPTMKOuXBiWKdIMg</token></avatar> */

		Element body = (Element) xml.getFirstChild();
		if (! body.getNodeName().equals("token")) {
			Log.d(TAG, "<avatar>'s first child is not <token>");
			return;
		}

		Bundle p = new Bundle();
		p.putString("token", getTextContent(body));

		postToNT("got-avatar-token", p);
	
	}

	public void
	process_message(Element xml)
	{
		/*
		<message from="malcom@tlen.pl/Resource" type="chat">
		    <body>tresc</body>
		    <x xmlns="jabber:x:delay" stamp="20070320T12:57:43"/>
		</message>
		 */

		String from = xml.getAttribute("from");
		String type = xml.getAttribute("type");

		if (! type.equals("chat")) {
			Log.e("XMLHandler", "process_message: type != chat");
			return;
		}

		String[] fromSplit;

		fromSplit = from.split("/");
		if (fromSplit.length != 2) {
			Log.e("XMLHandler", "process_message: split length != 2");
			return;
		}

		from = fromSplit[0];
	
		Element body = (Element) xml.getFirstChild();
		if (! body.getNodeName().equals("body")) {
			Log.e("XMLHandler", "process_message: first node != body");
			return;
		}

		Element x;
		try {
			x = (Element) body.getNextSibling();
		} catch (IndexOutOfBoundsException e) {
			x = null;
		}

		Bundle p = new Bundle();
	
		if (x != null) {
			p.putString("stamp", x.getAttribute("stamp"));
		}

		p.putString("from", from);	
		p.putString("msg", Protocol.decodeString(getTextContent(body)));

		postToNT("got-message", p);
	}

	public void
	process_presence(Element xml)
	{
		/*
		<presence from="pelotasplus@gmail.com"><show>available</show><status/></presence>
		<presence from="iwgroszek@tlen.pl"><show>available</show></presence>

		<presence from='vxel@tlen.pl'>
			<show>available</show>
			<status>www.sfora.pl+/+czy+on+podgryza+mu+ucho?</status>
			<avatar>
				<a type='0' md5='b3686efaa815cf82912213db5d1be85e'/>
			</avatar>
		</presence>
		 */
		String type = xml.getAttribute("type");
		String from = xml.getAttribute("from");

		// Log.d(TAG, "process_pres type=" + type + ", from=" + from);

		/* stan uzytkownika */
		if (from != "" && (type == "" || type.equals("unavailable"))) {
			Element params = (Element) xml.getFirstChild();
			Bundle p = new Bundle();
			p.putString("username", from);	
			p.putString("show", "unavailable");
			p.putString("status", "");
			p.putString("avatar_md5", "");
			p.putString("avatar_type", "");

			while (params != null) {
				if (params.getNodeName().equals("show")) {
					p.putString("show", getTextContent(params));
				} else if (params.getNodeName().equals("status")) {
					p.putString("status", Protocol.decodeString(getTextContent(params)));
				} else if (params.getNodeName().equals("avatar")) {
					Element avatar = (Element) params.getFirstChild();
					p.putString("avatar_md5", avatar.getAttribute("md5"));
					p.putString("avatar_type", avatar.getAttribute("type"));
				}

				/* try-catch dla 1.6 */
				try {
					params = (Element) params.getNextSibling();
				} catch (java.lang.IndexOutOfBoundsException exc) {
					params = null;
				}
			}

			postToNT("got-presence", p);		
		}
	}
	
	public void
	process_set(Element xml)
	{
		Element query = (Element) xml.getFirstChild();
		
		if (query != null) {
			Element item = (Element) query.getFirstChild();
			
			if (item != null) {
				String subscription = item.getAttribute("subscription");
				String jid = item.getAttribute("jid");

				if (subscription != "" && jid != "") {
					// potwierdzenie usuniecia uzytkownika
					/* <iq type="set"><query><item jid="pelotas@tlen.pl" subscription="remove"/></query></iq> */
					if ("remove".equals(subscription)) {
						Log.d(TAG, "User '" + jid + "' has been removed");

						Bundle b = new Bundle();
						b.putString("jid", jid);
						postToNT("got-user-removed", b);
					} else {
						Log.d(TAG, "GOT sub=" + subscription + ", jid=" + jid);
					}
				}
			}
		}
	}
	
	public void
	process_user_info(Element xml)
	{
		/*
			<iq type="result" to="pelotas@tlen.pl/Tlen" from="tuba" id="alewi@tlen.pl">
				<query>
					<item jid="alewi">
						<first>Agata+</first>
						<last>Lewi%F1ska</last>
						<nick>Agata</nick>
						<email>alewi%40go2.pl</email>
					</item>
				</query>
			</iq>

			empty one

			<iq type="result" to="pelotas@tlen.pl/Tlen" from="tuba" id="magdudek@tlen.pl">
				<query/>
			</iq>
		 */

		Bundle b = new Bundle();

		b.putString("id", xml.getAttribute("id"));

		Element query = (Element) xml.getFirstChild();
		if (query == null) {
			b.putString("empty-response", "1");
			postToNT("got-user-info", b);
		}

		Element item = (Element) query.getFirstChild();
		if (item == null) {
			b.putString("empty-response", "1");
			postToNT("got-user-info", b);
			return;
		}

		Element param = (Element) item.getFirstChild();

		while (param != null) {
			String name = param.getNodeName();
			String value = Protocol.decodeString(getTextContent(param));

			b.putString(name, value);

			/* na 1.6 mamy tu wyjatek java.lang.IndexOutOfBoundsException */
			try {
				param = (Element) param.getNextSibling();
			} catch (java.lang.IndexOutOfBoundsException exc) {
				param = null;
			}
		}

		postToNT("got-user-info", b);
	}

	public void
	process_roster(Element xml)
	{
/*
   <iq type="result" id="GetRoster">
	   <query xmlns="jabber:iq:roster">
		   <item jid="malcom@tlen.pl" name="MalCom" subscription="both">
			   <group>Kontakty</group>
		   </item>
		   <item jid="malcom21@tlen.pl" name="malcom21" subscription="none">
			   <group>Kontakty</group>
		   </item>
		   <item jid="david@tlen.pl" name="Dawid" subscription="to">
			   <group>Znajomi</group>
		   </item>
		   <item jid="contact@tlen.pl" name="ktos" subscription="none" ask="subscribe"/>
	   </query>
   </iq>
 */
		Element query = (Element) xml.getFirstChild();

		Log.d(TAG, "query=" + dom2str(query));

		Bundle roster = new Bundle();

		Element item = (Element) query.getFirstChild();
		while (item != null) {
			String jid = item.getAttribute("jid");
			String name = Protocol.decodeString(item.getAttribute("name"));
			String subscription = item.getAttribute("subscription");	

			/* looking for group */
			String group_name = null;
			Element group = (Element) item.getFirstChild();
			if (group != null && group.getNodeName().equals("group")) {
				group_name = getTextContent(group);
			}

			// Log.d(TAG, "jid=" + jid + ", name=" + name + ", subscription=" + subscription);

			ArrayList<String> buddy = new ArrayList<String>();
			buddy.add(jid);
			buddy.add(name);
			buddy.add(subscription);
			buddy.add(group_name);

			roster.putStringArrayList("ri:" + jid, buddy);

			/* na 1.6 mamy tu wyjatek java.lang.IndexOutOfBoundsException */
			try {
				item = (Element) item.getNextSibling();
			} catch (java.lang.IndexOutOfBoundsException exc) {
				item = null;
			}

		}

		postToNT("got-roster", roster);
	}

	public void
	process_xml(Element xml)
	{
		// Log.d(TAG, "PROCESS " + dom2str(xml));

		String name, id, type, from;

		name = xml.getNodeName();
		id   = xml.getAttribute("id");
		type = xml.getAttribute("type");
		from = xml.getAttribute("from");
		
		// Log.d(TAG, "name=" + name + ", id=" + id + ", type=" + type);

		
		// <iq type="set"><query><item jid="pkonopko@tlen.pl" subscription="remove"/></query></iq>
		
		if ("iq".equals(name)) {
			if (sessionId.equals(id)) {
				if ("result".equals(type)) {
					postToNT("got-password-correct");
				} else {
					postToNT("got-password-incorrect");
				}
			} else if ("GetRoster".equals(id)) {
				process_roster(xml);
			} else if ("tuba".equals(from) && "result".equals(type)) {
				process_user_info(xml);
			} else if ("set".equals(type)) {
				process_set(xml);
			}
		} else if ("presence".equals(name)) {
			process_presence(xml);
		} else if ("message".equals(name)) {
			process_message(xml);
		} else if ("avatar".equals(name)) {
			process_avatar_token(xml);
		}
	}

	public
	XMLHandler(Context ctx)
	{
		this.ctx = ctx;

		Log.d(TAG, "start XMLHandler");

		try {
			dFactory = DocumentBuilderFactory.newInstance();
			dBuilder = dFactory.newDocumentBuilder();
			dImpl    = dBuilder.getDOMImplementation();
			doc      = dImpl.createDocument(null, null, null);
		} catch (Exception e) {
			Log.e(TAG, "XMLHandler: exc " + e);
		}
	}

	@Override
	public void
	characters(char[] ch, int start, int length)
	throws SAXException
	{
		// Log.e("tlen", "characters " + ch);

		super.characters(ch, start, length);

		if (root == null)
			return;


		String data = new String(ch, start, length);
		setTextContent(root, data);
	}

	@Override
	public void
	endElement(String uri, String localName, String name)
	throws SAXException
	{
		// Log.e("XMLHandler", "endElement: " + localName + "; root=" + dom2str(root));

		super.endElement(uri, localName, name);

		/* pachnie koncowym tagiem S */
		if (root == null && localName.equals("s")) {
			Log.d(TAG, "got end of session tag");

			throw new SAXException("got-session-end");
			// postToNT("got-session-end");
			// return;
		}

		if (root == null) {
			Log.e("XMLHandler", "endElement without root -- ignoring");
			
			return;
		}

		if (root.getParentNode() != null) {
			if (root.getNodeName().equals(localName)) {
				root = (Element) root.getParentNode();
			}
		} else {
			process_xml(root);

			root = null;	
		}
	}

	@Override
	public void
	startDocument()
	throws SAXException
	{
		// Log.e("XMLHandler", "startDocument");

		super.startDocument();
	}

	
	String
	dom2str(Node el)
	{
		if (el == null)
			return "(null)";

		return "(requires 2.2)";

		/* requires 2.2 */
		/*
		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();

			
			ByteArrayOutputStream os = new ByteArrayOutputStream();

		
			DOMSource domSource = new DOMSource(el);
			StreamResult result = new StreamResult(os);

			transformer.transform(domSource, result);

			String ret = os.toString();
			ret = ret.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");

			return ret;
		} catch (Exception e) {
			Log.e("XMLHandler", "transform exception: " + e);

			return "(exception)";
		}
		 */
	}
	

	@Override
	public void
	startElement(String uri, String localName, String name, Attributes attributes)
	throws SAXException
	{
		// Log.e("XMLHandler", "startElement: " + localName +"; root=" + dom2str(root));

		super.startElement(uri, localName, name, attributes);

		Element element;

		element = doc.createElement(localName);

		int i;
		for (i = 0; i < attributes.getLength(); i++) {
			element.setAttribute(attributes.getLocalName(i), attributes.getValue(i));
		}

		if (element.getNodeName().equals("s") && root == null) {
			Log.d(TAG, "startElement " + dom2str(element));

			Bundle b = new Bundle();

			b.putString("session-id", element.getAttribute("i"));
			this.sessionId = b.getString("session-id");
			b.putString("k1",         element.getAttribute("k1"));
			b.putString("k2",         element.getAttribute("k2"));
			b.putString("k3",         element.getAttribute("k3"));

			postToNT("got-session-start", b);
			return;
		}

		if (root == null) {
			root = element;
		} else {
			root.appendChild(element);
			root = element;
		}
	}
}
