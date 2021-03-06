package ch.tarsier.tarsier.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * CompositeValidator is the class that represents a composite validator.
 *
 * @author romac
 * @param <T> The type of the elements to validate.
 */
public class CompositeValidator<T> extends AbstractValidator<T> {

    private List<Validator<T>> mValidators;

    public CompositeValidator() {
        this(new ArrayList<Validator<T>>());
    }

    public CompositeValidator(List<Validator<T>> validators) {
        mValidators = validators;
    }

    public void addValidator(Validator<T> validator) {
        mValidators.add(validator);
    }

    @Override
    protected boolean isValid(T t) {
        boolean allValid = true;
        for (Validator<T> validator : mValidators) {
            if (!validator.validate(t)) {
                if (validator.hasErrorMessage()) {
                    setErrorMessage(validator.getErrorMessage());
                    allValid = false;
                }

            }
        }
        return allValid;
    }

}
