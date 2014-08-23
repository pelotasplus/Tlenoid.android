package pl.com.nic.android.tlen;

public class
Presence
{
	private String name = null;
	private int id = -1;
	private String description = null;

	public
	Presence(String name)
	{
		this.id = name2id(this.name);
		this.name = name;
	}
	
	
	public
	Presence(String name_, String desc)
	{
		id = name2id(name_);
		name = name_;
		description = desc;
	}


	public
	Presence(int id)
	{
		this.id = id;
		this.name = id2name(this.id);
	}

	public
	Presence(int id, String desc)
	{
		this.id = id;
		this.name = id2name(this.id);
		this.description = desc;
	}

	public String
	toString()
	{
		return "Presence[id=" + this.id
			+ ", name=" + this.name
			+ ", desc=" + this.description
			+ "]";
	}

	private String
	id2name(int id)
	{	
		if (id == 0)
			return "Available";
		else if (id == 1)
			return "Away";
		else if (id == 2)
			return "Extended away";
		else if (id == 3)
			return "Do not disturb";
		else if (id == 4)
			return "Chatty";
		else if (id == 5)
			return "Invisible";
		else
			return "ERROR";
	}

	private int
	name2id(String name)
	{	
		if (name.equals("Available"))
			return 0;
		else if (name.equals("Away"))
			return 1;
		else if (name.equals("Extended away"))
			return 2;
		else if (name.equals("Do not disturb"))
			return 3;
		else if (name.equals("Chatty"))
			return 4;
		else if (name.equals("Invisible"))
			return 5;
		else
			return -1;
	}

	public int
	getDrawableId()
	{
		if (this.id == 0)
			return R.drawable.online;
		else if (this.id == 1)
			return R.drawable.away;
		else if (this.id == 2)
			return R.drawable.xa;
		else if (this.id == 3)
			return R.drawable.dnd;
		else if (this.id == 4)
			return R.drawable.chat;
		else if (this.id == 5)
			return R.drawable.unavailable;
		else
			return R.drawable.unauthorized;
	}

	public String
	getCode()
	{
		if (this.id == 0)
			return "available";
		else if (this.id == 1)
			return "away";
		else if (this.id == 2)
			return "xa";
		else if (this.id == 3)
			return "dnd";
		else if (this.id == 4)
			return "chat";
		else if (this.id == 5)
			return "invisible";
		else
			return "available";
	}

	public String
	getName()
	{
		return this.name;
	}

	public int
	getId()
	{
		return this.id;
	}

	public String
	getDesc()
	{
		return this.description;
	}
}
