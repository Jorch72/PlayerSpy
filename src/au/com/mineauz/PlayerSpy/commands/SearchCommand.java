package au.com.mineauz.PlayerSpy.commands;

import java.util.ArrayDeque;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import au.com.mineauz.PlayerSpy.Utilities.Pair;
import au.com.mineauz.PlayerSpy.fsa.*;
import au.com.mineauz.PlayerSpy.search.*;
import au.com.mineauz.PlayerSpy.search.interfaces.Constraint;

public class SearchCommand implements ICommand 
{
	private static State mStartState;
	static
	{
		setupFSA();
	}
	
	private static void setupFSA()
	{
		State terminator = new FinalCompactorDA().addNext(new FinalState());
		
		// Constraints
	
		State dateEnd = new DateConstraintDA().addNext(terminator);
		
		State beforeTimeState = new StringState("before")
			.addNext(new DateState()
				.addNext(new TimeState()
					.addNext(new DateCompactorDA()
						.addNext(dateEnd)
					)
				)
				.addNext(dateEnd)
			)
			.addNext(new TimeState()
				.addNext(new TimeOnlyDA()
					.addNext(dateEnd)
				)
			)
		;
		State afterTimeState = new StringState("after")
			.addNext(new DateState()
				.addNext(new TimeState()
					.addNext(new DateCompactorDA()
						.addNext(dateEnd)
					)
				)
				.addNext(dateEnd)
			)
			.addNext(new TimeState()
				.addNext(new TimeOnlyDA()
					.addNext(dateEnd)
				)
			)
		;
		
		State betweenPt2 = new StringState("and")
			.addNext(new DateState()
				.addNext(new TimeState()
					.addNext(new DateCompactorDA()
						.addNext(dateEnd)
					)
				)
				.addNext(dateEnd)
			)
			.addNext(new TimeState()
				.addNext(new TimeOnlyDA()
					.addNext(dateEnd)
				)
			)
		;
		
		State betweenTimeState = new StringState("between")
			.addNext(new DateState()
				.addNext(new TimeState()
					.addNext(new DateCompactorDA()
						.addNext(betweenPt2)
					)
				)
				.addNext(betweenPt2)
			)
			.addNext(new TimeState()
				.addNext(new TimeOnlyDA()
					.addNext(betweenPt2)
				)
			)
		;
		
		State dateConstraint = new NullState()
			.addNext(beforeTimeState)
			.addNext(afterTimeState)
			.addNext(betweenTimeState)
			.addNext(terminator)
		;
		
		State modifiers = new NullState()
			.addNext(new StringState("show")
				.addNext(new MultiStringState("location","locations")
					.addNext(new ShowLocationDA()
						.addNext(dateConstraint)
					)
				)
			)
			.addNext(dateConstraint)
		;
		
		State extraCauseConstraint = new StringState("or");
		
		extraCauseConstraint.addNext(new CauseState()
			.addNext(extraCauseConstraint)
			.addNext(modifiers)
		);
		
		
		State causeConstraint = new NullState()
			.addNext(new StringState("by")
				.addNext(new CauseState()
					.addNext(extraCauseConstraint)
					.addNext(modifiers)
				)
			)
			.addNext(modifiers)
		;
			
		
		
		State endOfActions = new NullState()
			.addNext(new StringState("within")
				.addNext(new IntState(0, Integer.MAX_VALUE)
					.addNext(new DistanceConstraintDA()
						.addNext(causeConstraint)
					)
				)
			)
			.addNext(causeConstraint)
		;
		
		
		// All block related ones
		State endOfBlockAction = new BlockConstraintDA().addNext(endOfActions);
		
		State anyBlock = new StringState("any").addNext(new StringState("block")
			.addNext(new AnyBlockDA()
				.addNext(endOfBlockAction)
			)
		);
		State painting = new StringState("painting").addNext(new PaintingDA().addNext(endOfBlockAction));
		
		State blockId = new TypeIdState(true,false).addNext(endOfBlockAction);
		
		// All entity related ones
		State entityId = new EntityTypeState().addNext(new EntityConstraintDA(false)
			.addNext(endOfActions)
		);
		
		State anyEntity = new StringState("any").addNext(new StringState("entity")
			.addNext(new AnyEntityDA()
				.addNext(new EntityConstraintDA(false)
					.addNext(endOfActions)
				)
			)
		);
		
		State playerName = new PlayerNameState().addNext(new EntityConstraintDA(true)
			.addNext(endOfActions)
		);
		State playerNameAlt = new StringState("player").addNext(new PlayerNameState()
			.addNext(new AltPlayerDA()
				.addNext(new EntityConstraintDA(true)
					.addNext(endOfActions)
				)
			)
		);
		
		// All chat / command related ones
		State endOfChatAction = new ChatCommandConstraintDA(false).addNext(causeConstraint);
		State chatCommandMatches = new StringState("contains").addNext(new StringState(null)
			.addNext(new ChatCommandConstraintDA(true)
				.addNext(causeConstraint)
			)
		);
		
		State transactionEnd = new TransactionDA().addNext(endOfActions);
		
		State transactionTarget = new NullState()
			.addNext(new TypeIdState(true,false)
				.addNext(transactionEnd)
			)
			.addNext(new EntityTypeState()
				.addNext(transactionEnd)
			)
			.addNext(new PlayerNameState()
				.addNext(transactionEnd)
			)
		;
			
		
		State takeState = new NullState()
			.addNext(new TypeIdState(true,true)
				.addNext(new IntState(0,Integer.MAX_VALUE)
					.addNext(new StringState("from")
						.addNext(transactionTarget)
					)
					.addNext(transactionEnd)
				)
				.addNext(new StringState("from")
					.addNext(transactionTarget)
				)
				.addNext(transactionEnd)
			)
			.addNext(new MultiStringState("any","anything")
				.addNext(new GenericDA(new Pair<Material,Integer>(Material.AIR,-1),1)
					.addNext(new IntState(0,Integer.MAX_VALUE)
						.addNext(new StringState("from")
							.addNext(transactionTarget)
						)
						.addNext(transactionEnd)
					)
					.addNext(new StringState("from")
						.addNext(transactionTarget)
					)
					.addNext(transactionEnd)
				)
			)
		;
		
		State giveState = new NullState()
			.addNext(new TypeIdState(true,true)
				.addNext(new IntState(0,Integer.MAX_VALUE)
					.addNext(new StringState("to")
						.addNext(transactionTarget)
					)
					.addNext(transactionEnd)
				)
				.addNext(new StringState("to")
					.addNext(transactionTarget)
				)
				.addNext(transactionEnd)
			)
			.addNext(new MultiStringState("any","anything")
				.addNext(new GenericDA(new Pair<Material,Integer>(Material.AIR,-1),1)
					.addNext(new IntState(0,Integer.MAX_VALUE)
						.addNext(new StringState("to")
							.addNext(transactionTarget)
						)
						.addNext(transactionEnd)
					)
					.addNext(new StringState("to")
						.addNext(transactionTarget)
					)
					.addNext(transactionEnd)
				)
			)
		;
		
		mStartState = new InitialState()
			.addNext(new StringState("for")
				.addNext(new MultiStringState("place","placed","placing")
					.addNext(blockId)
					.addNext(anyBlock)
					.addNext(painting)
				)
				.addNext(new MultiStringState("break","mine","mined","dug","destroyed","breaking","broken","removed","remove")
					.addNext(blockId)
					.addNext(anyBlock)
					.addNext(painting)
				)
				.addNext(new MultiStringState("take","took")
					.addNext(takeState)
				)
				.addNext(new MultiStringState("put","give","gave")
					.addNext(giveState)
				)
//				.addNext(new StringState("spawn")
//					.addNext(anyEntity)
//					.addNext(entityId)
//				)
				.addNext(new StringState("kill")
					.addNext(playerNameAlt)
					.addNext(anyEntity)
					.addNext(entityId)
					.addNext(playerName)
				)
				.addNext(new MultiStringState("command","cmd")
					.addNext(chatCommandMatches)
					.addNext(endOfChatAction)
					.addNext(terminator)
				)
				.addNext(new StringState("chat")
					.addNext(chatCommandMatches)
					.addNext(endOfChatAction)
					.addNext(terminator)
				)
			);
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
	public String getUsageString(String label) 
	{
		return label + " <args>. See docs for more info";
	}

	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length == 0)
			return false;
		
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
			ArrayDeque<Object> results = FiniteStateAutomata.parse(inputString.toLowerCase(), mStartState);
			SearchFilter filter = (SearchFilter)results.pop();
			
			for(Constraint constraint : filter.andConstraints)
			{
				if(constraint instanceof DistanceConstraint)
				{
					if(sender instanceof ConsoleCommandSender)
					{
						sender.sendMessage(ChatColor.RED + "You need to be a player to use 'within <range>'");
						return true;
					}
					((DistanceConstraint) constraint).location = ((Player)sender).getLocation().clone();
				}
			}
		
			sender.sendMessage(ChatColor.GREEN + "Searching...");
			Searcher.instance.searchAndDisplay(sender, filter);
		}
		catch(ParseException e)
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
