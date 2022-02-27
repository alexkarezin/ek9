package org.ek9lang.compiler.symbol;

import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AggregateWithTraitsSymbol extends AggregateSymbol
{
	/**
	 * The range of traits this aggregate extends/implements
	 */
	private final List<AggregateWithTraitsSymbol> traits = new ArrayList<>();

	/**
	 * The range of aggregates that are allowed to extend/implement this aggregate.
	 * Currently, used in a trait definition to limit the classes that can implement it.
	 * But could clearly be used elsewhere in the future.
	 */
	private final List<IAggregateSymbol> allowOnly = new ArrayList<>();

	public AggregateWithTraitsSymbol(String name, IScope enclosingScope)
	{
		super(name, enclosingScope);
	}

	public AggregateWithTraitsSymbol(String name, Optional<ISymbol> type, IScope enclosingScope)
	{
		super(name, type, enclosingScope);
	}

	@Override
	public AggregateWithTraitsSymbol clone(IScope withParentAsAppropriate)
	{
		return cloneIntoAggregateWithTraitsSymbol(new AggregateWithTraitsSymbol(this.getName(), this.getType(), withParentAsAppropriate));
	}

	protected AggregateWithTraitsSymbol cloneIntoAggregateWithTraitsSymbol(AggregateWithTraitsSymbol newCopy)
	{
		super.cloneIntoAggregateSymbol(newCopy);
		newCopy.traits.addAll(traits);
		newCopy.allowOnly.addAll(allowOnly);
		return newCopy;
	}

	@Override
	public double getAssignableWeightTo(ISymbol s)
	{
		//easy if same type and parameterization
		double canAssign = super.getAssignableWeightTo(s);
		if(canAssign >= 0.0)
			return canAssign;

		//if not then we have to cycle through the traits.
		for(IAggregateSymbol trait : traits)
		{
			canAssign = trait.getAssignableWeightTo(s);
			//Add some weight a bit more because it is a trait
			if(canAssign >= 0.0)
				return 0.05 + canAssign;
		}
		return -1000000.0;
	}

	public double getUnCoercedAssignableWeightTo(ISymbol s)
	{
		//easy if same type and parameterization
		double canAssign = super.getUnCoercedAssignableWeightTo(s);
		if(canAssign >= 0.0)
			return canAssign;

		//if not then we have to cycle through the traits.
		for(IAggregateSymbol trait : traits)
		{
			canAssign = trait.getUnCoercedAssignableWeightTo(s);
			//Add some weight a bit more because it is a trait
			if(canAssign >= 0.0)
				return 0.05 + canAssign;
		}
		return -1000000.0;
	}

	public double getAssignableWeightTo(Optional<ISymbol> s)
	{
		return s.map(this::getAssignableWeightTo).orElse(-100000.0);
	}

	public AggregateWithTraitsSymbol addTrait(AggregateWithTraitsSymbol traitSymbol)
	{
		AssertValue.checkNotNull("Trait cannot be null", traitSymbol);
		traits.add(traitSymbol);

		//Do we want this to be known as a sub aggregate?
		traitSymbol.addSubAggregateScopedSymbol(this);
		return this;
	}

	@Override
	public boolean isTraitImplemented(AggregateWithTraitsSymbol traitSymbol)
	{
		if(this == traitSymbol)
			return true;

		for(AggregateWithTraitsSymbol trait : traits)
			if(trait.isTraitImplemented(traitSymbol))
				return true;

		return super.isTraitImplemented(traitSymbol);
	}

	/**
	 * Get the traits that this aggregates implements.
	 *
	 * @return The list of traits.
	 */
	@Override
	public List<IAggregateSymbol> getTraits()
	{
		return new ArrayList<>(traits);
	}

	/**
	 * Gets all traits this class implements directly or indirectly through supers that have constraints upon them.
	 *
	 * @return The list of constraining traits - no implication or check on what they are constrained to - that's up to the caller.
	 */
	@Override
	public List<AggregateWithTraitsSymbol> getAllExtensionConstrainedTraits()
	{
		return getAllTraits().stream().filter(AggregateWithTraitsSymbol::isExtensionConstrained).collect(Collectors.toList());
	}

	/**
	 * Does this aggregate have the trait passed in as an immediate trait - i.e. one it implements itself.
	 * Note this is different from being compatible with the trait via hierarchy!
	 *
	 * @param trait The trait to check.
	 * @return true if it does false otherwise.
	 */
	public boolean hasImmediateTrait(IAggregateSymbol trait)
	{
		return traits.stream().anyMatch(ownTrait -> ownTrait.isExactSameType(trait));
	}

	/**
	 * Get all the traits this aggregate implements - this means all the traits any of these traits extends.
	 * But also going up the class supers and getting those traits and any of the traits they extend.
	 */
	@Override
	public List<AggregateWithTraitsSymbol> getAllTraits()
	{
		List<AggregateWithTraitsSymbol> rtn = new ArrayList<>();
		//Add our traits in and those the trait extends - this will then go all the way up the extending list
		traits.forEach(trait -> {
			if(!rtn.contains(trait))
			{
				rtn.add(trait);
				trait.getAllTraits().forEach(superTrait -> {
					if(!rtn.contains(superTrait))
						rtn.add(superTrait);
				});
			}
		});

		//So this might be a class itself - so go up the supers to get all those traits.
		if(getSuperAggregateScopedSymbol().isPresent())
		{
			IAggregateSymbol theSuper = getSuperAggregateScopedSymbol().get();
			List<AggregateWithTraitsSymbol> superTraits = theSuper.getAllTraits();
			superTraits.forEach(trait -> {
				if(!rtn.contains(trait))
					rtn.add(trait);
			});
		}

		return rtn;
	}

	/**
	 * Gets all abstract methods in this aggregate and any super classes
	 *
	 * @return A list of all the methods marked as abstract.
	 */
	@Override
	public List<MethodSymbol> getAllAbstractMethods()
	{

		List<MethodSymbol> rtn = super.getAllAbstractMethods();
		traits.forEach(trait -> rtn.addAll(trait.getAllAbstractMethods()));
		return rtn;
	}

	@Override
	public boolean isExtensionConstrained()
	{
		return !allowOnly.isEmpty();
	}

	/**
	 * Is the extender allowed to implement this aggregate (normally a trait)
	 *
	 * @param extenderSymbol The class (normally the one we are testing)
	 * @return true if extending is allowed.
	 */
	public boolean isAllowingExtensionBy(IAggregateSymbol extenderSymbol)
	{
		if(!isExtensionConstrained())
			return true;

		return allowOnly.contains(extenderSymbol);
	}

	public AggregateWithTraitsSymbol addAllowedExtender(IAggregateSymbol extenderSymbol)
	{
		AssertValue.checkNotNull("AllowedExtender cannot be null", extenderSymbol);
		allowOnly.add(extenderSymbol);

		return this;
	}

	public List<IAggregateSymbol> getAllowedExtenders()
	{
		return new ArrayList<>(allowOnly);
	}

	@Override
	public List<MethodSymbol> getAllNonAbstractMethods()
	{
		List<MethodSymbol> rtn = super.getAllNonAbstractMethods();
		for(IAggregateSymbol trait : traits)
		{
			rtn.addAll(trait.getAllNonAbstractMethods());
		}
		return rtn;
	}

	@Override
	public MethodSymbolSearchResult resolveForAllMatchingMethods(MethodSymbolSearch search, MethodSymbolSearchResult result)
	{
		//System.out.println("Search '" + this.getName() + "' AggregateWithTraitsSymbol: " + search);

		MethodSymbolSearchResult buildResult = new MethodSymbolSearchResult(result);
		//Do traits first then do own and our supers - so with the traits we consider them 'peers' and so need to merge the result
		//So this would give us potentially two methods with same signature in our results - which is what we need to see.
		for(IAggregateSymbol trait : traits)
		{
			buildResult = buildResult.mergePeerToNewResult(trait.resolveForAllMatchingMethods(search, new MethodSymbolSearchResult()));
			//System.out.println(this.getName() + ": AggregateWithTraitsSymbol after " + trait.getName()+ ": " + buildResult);
		}
		//But now we need to find the results for our own scope and classes we inherit from
		//But the key here is that when this scope provides an implementation it overrides zero, one or more implementations from the traits

		//No sure it this is right actually the super class is really a peer with the trait in terms of method resolution		
		buildResult = buildResult.overrideToNewResult(super.resolveForAllMatchingMethods(search, new MethodSymbolSearchResult()));

		//System.out.println("End Search '" + this.getName() + "' AggregateWithTraitsSymbol: " + search + ": " + buildResult);
		return buildResult;
	}

	@Override
	public Optional<ISymbol> resolveMember(SymbolSearch search)
	{
		//First check if in immediate class or super class
		Optional<ISymbol> rtn = super.resolveMember(search);
		if(rtn.isEmpty())
			rtn = resolveInTraits(search);
		return rtn;
	}

	private Optional<ISymbol> resolveInTraits(SymbolSearch search)
	{
		Optional<ISymbol> rtn = super.resolveMember(search);
		for(AggregateWithTraitsSymbol trait : traits)
		{
			rtn = trait.resolveMember(search);
			if(rtn.isPresent())
				return rtn;
		}
		return rtn;
	}

	@Override
	public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search)
	{
		//Now we also have to account for captured variables as well.

		return super.resolveInThisScopeOnly(search);
	}

	/**
	 * Resolve using super aggregate and or any traits we have.
	 * Look in our traits first then go to the normal AggregateSymbol resolution.
	 */
	protected Optional<ISymbol> resolveWithParentScope(SymbolSearch search)
	{
		Optional<ISymbol> rtn = resolveInTraits(search);

		if(rtn.isEmpty())
			rtn = super.resolveWithParentScope(search);
		return rtn;
	}
}
