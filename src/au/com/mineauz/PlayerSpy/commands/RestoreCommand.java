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

public class RestoreCommand implements ICommand
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
		return "restore";
	}

	@Override
	public String[] getAliases()
	{
		return null;
	}

	@Override
	public String getPermission()
	{
		return "playerspy.restore";
	}

	@Override
	public String[] getUsageString( String label, CommandSender sender )
	{
		return new String[] {label + " [options]"};
	}

	@Override
	public String getDescription()
	{
		return "Restores data. If no options are specified, ALL data will be restored.";
	}
	
	
	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean canBeCommandBlock() { return false; }
	
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public boolean onCommand( CommandSender sender, String label, String[] args )
	{
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
				RollbackManager.instance.startRestore(filter);
			}
			else
			{
				RollbackManager.instance.startRestore(filter, (Player)sender, preview);
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
