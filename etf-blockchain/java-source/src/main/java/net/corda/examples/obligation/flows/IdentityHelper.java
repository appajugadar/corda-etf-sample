package net.corda.examples.obligation.flows;

import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;

public class IdentityHelper {
	
	public static Party getPartyWithName(Iterable<PartyAndCertificate> partyAndCertificates, String partyName) {
		for (PartyAndCertificate party : partyAndCertificates) {
			System.out.println("Party " + party.getParty()+"Name " + party.getParty().getName().getOrganisation());
			if (party.getName().getOrganisation().contains(partyName)) {
				return party.getParty();
			}
		}
		return null;
	}
}
