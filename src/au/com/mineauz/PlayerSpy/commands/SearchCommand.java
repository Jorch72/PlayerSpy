package au.com.mineauz.PlayerSpy.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.search.*;
import au.com.mineauz.PlayerSpy.search.AttributeParser.ParsedAttribute;
import au.com.mineauz.PlayerSpy.search.interfaces.CauseConstraint;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;
import au.com.mineauz.PlayerSpy.search.interfaces.IConstraint;

public class SearchCommand implements ICommand 
{
	private static AttributeParser mParser;
	
	static
	{
		mParser = new AttributeParser();
		Modifier notModifier = new Modifier("not", "!");
		
		mParser.addAttribute(new TypeAttribute().addModifier(notModifier));
		mParser.addAttribute(new NamedAttribute("filter",AttributeValueType.Sentence, "f:").addModifier(notModifier).setSingular(false));
		mParser.addAttribute(new NamedAttribute("dist",AttributeValueType.Number, "d:").addModifier(notModifier));
		mParser.addAttribute(new NamedAttribute("by",AttributeValueType.String, "@").addModifier(notModifier).setSingular(false));
		mParser.addAttribute(new NamedAttribute("after",AttributeValueType.Date, "ts:"));
		mParser.addAttribute(new NamedAttribute("before",AttributeValueType.Date, "te:"));
	}

	@Override
	public String getName() 
	{
		return "search";
	}

	@Override
	public String[] getAliases() 
	{
		return null;
	}

	@Override
	public String getPermission() 
	{
		return "playerspy.search";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) 
	{
		return new String[] {label + " <args>" };
	}

	@Override
	public String getDescription()
	{
		return "Searches through the databases to find relavent records.";
	}
	@Override
	public boolean canBeConsole() { return true; }
	
	@Override
	public boolean canBeCommandBlock() { return false; }

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		// Try page
		if(args.length == 1)
		{
			try
			{
				int page = Integer.parseInt(args[0]);
				
				if(!Searcher.instance.hasResults(sender, true))
				{
					sender.sendMessage(ChatColor.RED + "No results to display because no search has been done.");
					return true;
				}
				
				Searcher.instance.displayResults(sender, page-1);
				return true;
			}
			catch(NumberFormatException e)
			{
				
			}
		}
		
		// Collapse the args into a single string
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
			
			Searcher.instance.searchAndDisplay(sender, filter);
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
