package org.ek9lang.compiler.symbol.support;

import junit.framework.TestCase;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ParameterisedTypeSymbol;
import org.ek9lang.compiler.symbol.support.search.TemplateTypeSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.TypeSymbolSearch;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Going to be very hard to get your head around this as generics and
 * parameterised polymorphism is hard.
 * Especially templates that use templates.
 */
public class GenericTypeTest extends AbstractSymbolTestBase
{
	@Test
	public void testTemplateTypeCreation()
	{
		//The type T we are going to use as part of the template type definition
		var t = support.createGenericT("Tee", symbolTable);

		//Now the actual template type that can be parameterised.
		var z = support.createTemplateGenericType("Zee", symbolTable, t);
		symbolTable.define(z);

		//Now make sure it is possible to find the correct index of 'Tee'
		//This will be needed when we come to process parameterised types.
		TestCase.assertEquals(0, CommonParameterisedTypeDetails.getIndexOfType(z, Optional.of(t)));

		//Check we CANNOT find this as a type, but only as a template type.
		TestCase.assertTrue(symbolTable.resolve(new TypeSymbolSearch("Zee")).isEmpty());
		TestCase.assertTrue(symbolTable.resolve(new TypeSymbolSearch("Tee")).isEmpty());

		TestCase.assertTrue(z.isGenericInNature());
		TestCase.assertTrue(symbolTable.resolve(new TemplateTypeSymbolSearch("Zee")).isPresent());
		TestCase.assertEquals("Zee of Tee", z.getFriendlyName());

		//Now lets use that generic Z with a String and then with an Integer

		var stringZeeType = new ParameterisedTypeSymbol(z, symbolTable.resolve(new TypeSymbolSearch("String")), symbolTable);
		symbolTable.define(stringZeeType);
		TestCase.assertEquals("Zee of String", stringZeeType.getFriendlyName());
		//It is now a real concrete type and not generic in nature.
		TestCase.assertFalse(stringZeeType.isGenericInNature());
		//But it is actually a parameterised type
		TestCase.assertTrue(stringZeeType.isAParameterisedType());

		var integerZeeType = new ParameterisedTypeSymbol(z, symbolTable.resolve(new TypeSymbolSearch("Integer")), symbolTable);
		symbolTable.define(integerZeeType);
		TestCase.assertEquals("Zee of Integer", integerZeeType.getFriendlyName());
	}

	@Test
	public void testMatchingListsOfTypes()
	{
		var dateType = symbolTable.resolve(new TypeSymbolSearch("Date"));
		var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		TestCase.assertTrue(dateType.isPresent());
		TestCase.assertTrue(stringType.isPresent());

		TestCase.assertFalse(CommonParameterisedTypeDetails
				.doSymbolsMatch(
						List.of(dateType.get()), null
				)
		);
		TestCase.assertFalse(CommonParameterisedTypeDetails
				.doSymbolsMatch(
						null, List.of(dateType.get())
				)
		);

		TestCase.assertFalse(CommonParameterisedTypeDetails
				.doSymbolsMatch(
						List.of(dateType.get()), List.of(stringType.get())
				)
		);

		TestCase.assertFalse(CommonParameterisedTypeDetails
				.doSymbolsMatch(
						List.of(dateType.get(), stringType.get()), List.of(stringType.get())
				)
		);

		TestCase.assertFalse(CommonParameterisedTypeDetails
				.doSymbolsMatch(
						List.of(dateType.get(), stringType.get()), List.of(stringType.get(), dateType.get())
				)
		);

		TestCase.assertTrue(CommonParameterisedTypeDetails
				.doSymbolsMatch(
						List.of(dateType.get()),List.of(dateType.get())
				)
		);

		TestCase.assertTrue(CommonParameterisedTypeDetails
				.doSymbolsMatch(
						List.of(stringType.get(), dateType.get()), List.of(stringType.get(), dateType.get())
				)
		);
	}

	/**
	 * A quick check of some type that can be parameterised with multiple types.
	 * <p>
	 * We've got multiple uses and reuses of generic types in here.
	 */
	@Test
	public void testMultipleT()
	{
		var dateType = symbolTable.resolve(new TypeSymbolSearch("Date"));
		var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
		var booleanType = symbolTable.resolve(new TypeSymbolSearch("Boolean"));

		TestCase.assertTrue(dateType.isPresent());
		TestCase.assertTrue(stringType.isPresent());
		TestCase.assertTrue(integerType.isPresent());
		TestCase.assertTrue(booleanType.isPresent());

		var p = support.createGenericT("Pee", symbolTable);
		var q = support.createGenericT("Que", symbolTable);
		var r = support.createGenericT("Arr", symbolTable);

		var x = support.createTemplateGenericType("Ex", symbolTable, List.of(p, q, r));
		symbolTable.define(x);

		var y = support.createTemplateGenericType("Why", symbolTable, List.of(p, r));
		symbolTable.define(y);

		//Check we can find the types that ave been used and cannot find the ones that haven't.
		TestCase.assertEquals(0, CommonParameterisedTypeDetails.getIndexOfType(y, Optional.of(p)));
		TestCase.assertEquals(1, CommonParameterisedTypeDetails.getIndexOfType(y, Optional.of(r)));
		TestCase.assertEquals(-1, CommonParameterisedTypeDetails.getIndexOfType(y, Optional.of(q)));

		var z = support.createTemplateGenericType("Zee", symbolTable, List.of(r, q, p));
		symbolTable.define(z);

		//OK is your mind blown now?
		//We have a mix of real fixed types and 'T' types and also templates defined in terms of templates.

		//Expect this to fail as it needs 3 parameters.
		try
		{
			new ParameterisedTypeSymbol(z, stringType, symbolTable);
			TestCase.fail("Expected incompatible number of parameters");
		}
		catch(RuntimeException th)
		{
			//Expect an exception
		}

		//Now lets try using some of those templates to make concrete types
		//but also parameterised types, where some parameters are still generic
		var theZType = new ParameterisedTypeSymbol(z, List.of(stringType.get(), integerType.get(), booleanType.get()), symbolTable);
		//z -> r, q, p so ZType -> String, Integer, Boolean: Hence r -> String, p -> Boolean, q -> Integer
		symbolTable.define(theZType);

		{
			//Make a parameterised type but with generic T's as if it is created within theZType - so it's still not concrete.
			var yDash = new ParameterisedTypeSymbol(y, List.of(p, r), symbolTable);

			//Now we need to see if we could work out what actual types should really be used with yDash
			var types = support.getSuitableParameters(theZType, yDash);
			//So what do we expect here?
			//whereas y -> p, r: When defined in the context of Z: we expect p -> Boolean, r -> String
			//System.out.println("types " + types);
			TestCase.assertEquals("Boolean", types.get(0).getName());
			TestCase.assertEquals("String", types.get(1).getName());
			//Now we would be able to create a real concrete type!
			//This in effect what our compiler will have to do, when processing generic type definitions
			//that actually use other generics within them. When the outer definition is made 'manifest'
			//the compiler has to make manifest all the other uses of other generic types that use both real
			//and conceptual generic 'T' parameters.
			var yDashDash = new ParameterisedTypeSymbol(y, types, symbolTable);
			TestCase.assertEquals("Why of (Boolean, String)", yDashDash.getFriendlyName());
		}

		//Now lets try and 'X' but mix real Types and conceptual 'T'
		{
			var xDash = new ParameterisedTypeSymbol(x, List.of(r, r, dateType.get()), symbolTable);
			var types = support.getSuitableParameters(theZType, xDash);
			//So what do we expect here?
			//whereas x -> p, q, r: But xDash: p -> r, q -> r, r -> "Date"
			// When defined in the context of Z: we expect p -> String, q -> String - but r is fixed on "Date"
			//System.out.println("types " + types);
			TestCase.assertEquals("String", types.get(0).getName());
			TestCase.assertEquals("String", types.get(1).getName());
			TestCase.assertEquals("Date", types.get(2).getName());
			var xDashDash = new ParameterisedTypeSymbol(x, types, symbolTable);
			TestCase.assertEquals("Ex of (String, String, Date)", xDashDash.getFriendlyName());
		}

	}

	/**
	 * Designed to test out if we can work out the concrete types
	 * when using parameterised types within a parameterised type.
	 * i.e. G of (S, T) for example, then inside type G we use S or T in another template type.
	 * i.e. lets say with G we use a List of S or a Function of T. This means when we come to use
	 * actual types like String or Integer for S and T, we must put those in place in G.
	 * But in some cases within G of (S, T) we might use a List of Date i.e. we don;t need to replace!
	 * <p>
	 * HARD, very HARD (well for me anyway).
	 */
	@Test
	public void testCommonT()
	{
		var t = support.createGenericT("Tee", symbolTable);
		var z = support.createTemplateGenericType("Zee", symbolTable, t);
		symbolTable.define(z);

		//Now a new type but also refers to the same 'Tee' i.e. feels like it is defined within Zee
		//So the conceptual 'Tee' we're talking about here is the same 'Tee'
		var p = support.createTemplateGenericType("Pee", symbolTable, t);
		symbolTable.define(p);

		//Now make a parameterised type but not with a concrete type but with Tee again.
		var pDash = new ParameterisedTypeSymbol(p, Optional.of(t), symbolTable);

		var stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
		var integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));

		TestCase.assertTrue(stringType.isPresent());
		TestCase.assertTrue(integerType.isPresent());

		var stringZeeType = new ParameterisedTypeSymbol(z, stringType, symbolTable);
		symbolTable.define(stringZeeType);

		var integerZeeType = new ParameterisedTypeSymbol(z, integerType, symbolTable);
		symbolTable.define(integerZeeType);

		var types = support.getSuitableParameters(stringZeeType, pDash);

		TestCase.assertFalse(types.isEmpty());
		TestCase.assertTrue(types.get(0).isExactSameType(stringType.get()));

		var concreteTypes = support.getSuitableParameters(stringZeeType, integerZeeType);

		TestCase.assertFalse(concreteTypes.isEmpty());
		TestCase.assertTrue(concreteTypes.get(0).isExactSameType(integerType.get()));
	}
}
