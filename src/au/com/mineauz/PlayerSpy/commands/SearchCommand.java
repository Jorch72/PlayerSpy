package au.com.mineauz.PlayerSpy.commands;

import java.util.ArrayDeque;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import au.com.mineauz.PlayerSpy.fsa.*;
import au.com.mineauz.PlayerSpy.search.*;

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
		State dateConstraintDa = new DateConstraintDA(false).addNext(terminator);
		State dateConstraintDaDouble = new DateConstraintDA(true).addNext(terminator);
	
		State dateConstraint = new NullState()
			.addNext(new StringState("before")
				.addNext(new DateState()
					.addNext(new TimeState()
						.addNext(new DateCompactorDA()
							.addNext(dateConstraintDa)
						)
					)
					.addNext(dateConstraintDa)
				)
			)
			.addNext(new StringState("after")
				.addNext(new DateState()
					.addNext(new TimeState()
						.addNext(new DateCompactorDA()
							.addNext(dateConstraintDa)
						)
					)
					.addNext(dateConstraintDa)
				)
			)
			.addNext(new StringState("between")
				.addNext(new DateState()
					.addNext(new TimeState()
						.addNext(new DateCompactorDA()
							.addNext(new StringState("and")
								.addNext(new DateState()
									.addNext(new TimeState()
										.addNext(new DateCompactorDA()
											.addNext(dateConstraintDaDouble)
										)
									)
									.addNext(dateConstraintDaDouble)
								)
							)
						)
					)
					.addNext(new StringState("and")
						.addNext(new DateState()
							.addNext(new TimeState()
								.addNext(new DateCompactorDA()
									.addNext(dateConstraintDaDouble)
								)
							)
							.addNext(dateConstraintDaDouble)
						)
					)
				)
			);
		
		
		State extraPlayerConstraint = new StringState("or");
		
		extraPlayerConstraint.addNext(new PlayerNameState()
			.addNext(new PlayerConstraintDA()
				.addNext(extraPlayerConstraint)
				.addNext(dateConstraint)
			)
		);
		
		
		State playerConstraint = new NullState()
			.addNext(new StringState("by")
				.addNext(new PlayerNameState()
					.addNext(new PlayerConstraintDA()
						.addNext(extraPlayerConstraint)
						.addNext(dateConstraint)
					)
				)
			)
			.addNext(dateConstraint)
		;
			
		
		
		State endOfActions = new NullState()
			.addNext(new StringState("within")
				.addNext(new IntState(0, Integer.MAX_VALUE)
					.addNext(new DistanceConstraintDA()
						.addNext(playerConstraint)
					)
				)
			)
			.addNext(playerConstraint)
		;
		
		
		// All block related ones
		State endOfBlockAction = new BlockActionDA().addNext(endOfActions);
		
		State anyBlock = new StringState("any").addNext(new StringState("block")
			.addNext(new AnyBlockDA()
				.addNext(endOfBlockAction)
			)
		);
		State blockId = new BlockIdState().addNext(endOfBlockAction);
		
		// All entity related ones
		State entityId = new EntityTypeState().addNext(new EntityActionDA(false)
			.addNext(endOfActions)
		);
		
		State anyEntity = new StringState("any").addNext(new StringState("entity")
			.addNext(new AnyEntityDA()
				.addNext(new EntityActionDA(false)
					.addNext(endOfActions)
				)
			)
		);
		
		State playerName = new PlayerNameState().addNext(new EntityActionDA(true)
			.addNext(endOfActions)
		);
		State playerNameAlt = new StringState("player").addNext(new PlayerNameState()
			.addNext(new AltPlayerDA()
				.addNext(new EntityActionDA(true)
					.addNext(endOfActions)
				)
			)
		);
		
		// All chat / command related ones
		State endOfChatAction = new ChatCommandActionDA(false).addNext(playerConstraint);
		State chatCommandMatches = new StringState("contains").addNext(new StringState(null)
			.addNext(new ChatCommandActionDA(true)
				.addNext(playerConstraint)
			)
		);
		
		
		
		mStartState = new InitialState()
			.addNext(new StringState("for")
				.addNext(new MultiStringState("place","placed","placing")
					.addNext(blockId)
					.addNext(anyBlock)
				)
				.addNext(new MultiStringState("break","mine","mined","dug","destroyed","breaking","broken","removed")
					.addNext(blockId)
					.addNext(anyBlock)
				)
				.addNext(new StringState("spawn")
					.addNext(anyEntity)
					.addNext(entityId)
				)
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
		return label + " for <args>. See docs for more info";
	}

	@Override
	public boolean canBeConsole() { return true; }

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) 
	{
		if(args.length == 0)
			return false;
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
			
			if(sender instanceof ConsoleCommandSender)
				// Check for illegal within constraint
			{
				for(Constraint constraint : filter.constraints)
				{
					if(constraint instanceof DistanceConstraint)
					{
						sender.sendMessage(ChatColor.RED + "You need to be a player to use 'within <range>'");
						return true;
					}
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

}
