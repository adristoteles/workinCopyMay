package ca.wilkinsonlab.sadi.registry.test;

import org.sadiframework.SADIException;
import org.sadiframework.service.validation.ServiceValidator;
import org.sadiframework.service.validation.ValidationResult;

public class ValidateService
{
	public static void main(String[] args)
	{
		for (String service: args) {
			System.out.println(service);
			try {
				ValidationResult result = ServiceValidator.validateService(service);
				System.out.println("\t" + result.getWarnings());
			} catch (SADIException e) {
				e.printStackTrace();
			}
		}
	}
}
