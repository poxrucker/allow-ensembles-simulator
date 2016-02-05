package allow.simulator.netlogo.commands;

import org.nlogo.agent.Agent;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;

import allow.simulator.netlogo.agent.IAgent;

/**
 * NetLogo command to initiate knowledge exchange.
 * 
 * @author Andreas Poxrucker (DFKI)
 *
 */
public class ExchangeKnowledge extends DefaultCommand {

	@Override
	public void perform(Argument[] args, Context context) throws ExtensionException, LogoException {
		Agent a = (Agent) context.getAgent();
		
		if (a instanceof IAgent) {
			IAgent p = (IAgent) a;
			
			if (!p.getEntity().getFlow().isIdle()) {
				p.exchangeKnowledge();
			}
			
		} else {
			throw new ExtensionException("Error: Calling agent must be a valid Allow entity.");
		}
		
	}

}