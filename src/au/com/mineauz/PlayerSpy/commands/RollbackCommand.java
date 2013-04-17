package au.com.mineauz.PlayerSpy.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.attributes.AttributeParser;
import au.com.mineauz.PlayerSpy.attributes.AttributeValueType;
import au.com.mineauz.PlayerSpy.attributes.Modifier;
import au.com.mineauz.PlayerSpy.attributes.NamedAttribute;
import au.com.mineauz.PlayerSpy.attributes.RollbackTypeAttribute;
import au.com.mineauz.PlayerSpy.attributes.AttributeParser.ParsedAttribute;
import au.com.mineauz.PlayerSpy.rollback.RollbackManager;
import au.com.mineauz.PlayerSpy.rollback.RollbackSession;
import au.com.mineauz.PlayerSpy.search.CompoundConstraint;
import au.com.mineauz.PlayerSpy.search.DistanceConstraint;
import au.com.mineauz.PlayerSpy.search.FilterCauseConstraint;
import au.com.mineauz.PlayerSpy.search.FilterConstraint;
import au.com.mineauz.PlayerSpy.search.NotConstraint;
import au.com.mineauz.PlayerSpy.search.SearchFilter;
import au.com.mineauz.PlayerSpy.search.TimeConstraint;
import au.com.mineauz.PlayerSpy.search.interfaces.CauseConstraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;
import au.com.mineauz.PlayerSpy.search.interfaces.IConstraint;

public class RollbackCommand implements ICommand
{
	private static AttributeParser mParser;
	
	static
	{
		mParser = new AttributeParser();
		Modifier notModifier = new Modifier("not", "!");
		
		mParser.addAttribute(new RollbackTypeAttribute().addModifier(notModifier));
		mParser.addAttribute(new NamedAttribute("filter",AttributeValueType.Sentence, "f:").addModifier(notModifier).setSingular(false));
		mParser.addAttribute(new NamedAttribute("dist",AttributeValueType.Number, "d:").addModifier(notModifier));
		mParser.addAttribute(new NamedAttribute("by",AttributeValueType.String, "@").addModifier(notModifier).setSingular(false));
		mParser.addAttribute(new NamedAttribute("after",AttributeValueType.Date, "ts:"));
		mParser.addAttribute(new NamedAttribute("before",AttributeValueType.Date, "te:"));
		mParser.addAttribute(new NamedAttribute("preview",AttributeValueType.Null));
	}
	
	@Override
	public String getName()
	{
		return "rollback";
	}

	@Override
	public String[] getAliases()
	{
		return new String[] {"rb"};
	}

	@Override
	public String getPermission()
	{
		return "playerspy.rollback";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label + " [undo|<options>]"};
	}

	@Override
	public String getDescription()
	{
		return "Rolls back data. If no options are specified, ALL data will be rolled back. Specifying undo will restore the last rollback you did.";
	}
	
	
	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean canBeCommandBlock() { return false; }
	
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
		if(args.length == 1 && args[0].equalsIgnoreCase("undo"))
		{
			Player who = null;
			if(sender instanceof Player)
				who = (Player)sender;
			
			RollbackSession session = RollbackManager.instance.getLastRollbackSessionFor(who);
			if(session == null)
			{
				sender.sendMessage(ChatColor.RED + "There is nothing to undo");
				return true;
			}
			RollbackManager.instance.undoRollback(session, who);
			return true;
		}
		
		// Collapse the args into a single string
		boolean preview = false;
		String inputString = "";
		for(int i = 0; i < args.length; i++)
		{
			if(i != 0)
				inputString += " ";
			inputString += args[i];
		}
		
		try
		{
			List<ParsedAttribute> attributes = mParser.parse(inputString);
			
			ArrayList<Constraint> constraints = new ArrayList<Constraint>();
			ArrayList<CauseConstraint> causeConstraints = new ArrayList<CauseConstraint>();
			ArrayList<au.com.mineauz.PlayerSpy.search.interfaces.Modifier> modifiers = new ArrayList<au.com.mineauz.PlayerSpy.search.interfaces.Modifier>();
			for(ParsedAttribute res : attributes)
			{
				IConstraint<?> constraint = null;
				
				if(res.source.getName().equals("type"))
					constraint = new CompoundConstraint(false,(ArrayList<Constraint>)res.value);
				else if(res.source.getName().equals("dist"))
				{
					Location loc = null;
					if(sender instanceof Player)
						loc = ((Player)sender).getLocation();
					
					if(loc == null)
					{
						sender.sendMessage(ChatColor.RED + "You must be in-game to use the distance attribute.");
						return true;
					}
					
					constraint = new DistanceConstraint((Double)res.value, loc);
				}
				else if(res.source.getName().equals("filter"))
					constraint = new FilterConstraint((String)res.value);
				else if(res.source.getName().equals("after"))
					constraint = new TimeConstraint((Long)res.value, true);
				else if(res.source.getName().equals("before"))
					constraint = new TimeConstraint((Long)res.value, false);
				else if(res.source.getName().equals("by"))
					constraint = new FilterCauseConstraint((String)res.value);
				else if(res.source.getName().equals("preview"))
					preview = true;
				
				if(constraint == null)
					continue;
				
				// Apply modifiers
				for(Modifier mod : res.appliedModifiers)
				{
					if(mod.getName().equals("not"))
					{
						if(constraint != null)
							constraint = new NotConstraint(constraint);
					}
				}
				
				if(constraint instanceof Constraint)
					constraints.add((Constraint)constraint);
				if(constraint instanceof CauseConstraint)
					causeConstraints.add((CauseConstraint)constraint);
			}
			
			SearchFilter filter = new SearchFilter();
			filter.andConstraints = constraints;
			filter.causes = causeConstraints;
			filter.modifiers = modifiers;
			filter.noLimit = true;
			
			//filter.modifiers.add(new EndResultOnlyModifier());
			
			if(!(sender instanceof Player))
			{
				RollbackManager.instance.startRollback(filter);
			}
			else
			{
				RollbackManager.instance.startRollback(filter, (Player)sender, preview);
			}
		}
		catch(IllegalArgumentException e)
		{
			sender.sendMessage(ChatColor.RED + e.getMessage());
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete( CommandSender sender, String label, String[] args )
	{
		return null;
	}

}
