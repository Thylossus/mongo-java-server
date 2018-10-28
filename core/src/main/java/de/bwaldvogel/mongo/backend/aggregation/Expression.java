package de.bwaldvogel.mongo.backend.aggregation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import de.bwaldvogel.mongo.backend.Utils;
import de.bwaldvogel.mongo.bson.Document;
import de.bwaldvogel.mongo.exception.MongoServerError;
import de.bwaldvogel.mongo.exception.MongoServerException;

class Expression {

    static Object evaluate(Object expression, Document document) throws MongoServerException {
        if (expression instanceof String && ((String) expression).startsWith("$")) {
            String value = ((String) expression).substring(1);
            return Utils.getSubdocumentValue(document, value);
        } else if (expression instanceof Document) {
            Document result = new Document();
            for (Map.Entry<String, Object> entry : ((Document) expression).entrySet()) {
                String expressionKey = entry.getKey();
                Object expressionValue = entry.getValue();
                if (expressionKey.startsWith("$")) {
                    if (((Document) expression).keySet().size() > 1) {
                        throw new MongoServerError(15983, "An object representing an expression must have exactly one field: " + expression);
                    }
                    switch (expressionKey) {
                        case "$abs":
                            return evaluateAbsValue(expressionValue, document);
                        case "$sum":
                            return evaluateSumValue(expressionValue, document);
                        case "$subtract":
                            return evaluateSubtractValue(expressionValue, document);
                        default:
                            throw new MongoServerError(168, "InvalidPipelineOperator", "Unrecognized expression '" + expressionKey + "'");
                    }
                } else {
                    result.put(expressionKey, evaluate(expressionValue, document));
                }
            }
            return result;
        } else {
            return expression;
        }
    }

    private static Number evaluateAbsValue(Object expressionValue, Document document) throws MongoServerException {
        Object value = evaluate(expressionValue, document);
        if (value == null) {
            return null;
        } else if (value instanceof Double) {
            return Math.abs(((Double) value).doubleValue());
        } else if (value instanceof Long) {
            return Math.abs(((Long) value).longValue());
        } else if (value instanceof Integer) {
            return Math.abs(((Integer) value).intValue());
        } else {
            throw new MongoServerError(28765, "$abs only supports numeric types, not " + value.getClass());
        }
    }

    private static Number evaluateSumValue(Object expressionValue, Document document) throws MongoServerException {
        Object value = evaluate(expressionValue, document);
        if (value instanceof Number) {
            return (Number) value;
        } else if (value instanceof Collection) {
            Number sum = 0;
            Collection<?> collection = (Collection<?>) value;
            for (Object v : collection) {
                Object evaluatedValue = evaluate(v, document);
                if (evaluatedValue instanceof Number) {
                    sum = Utils.addNumbers(sum, (Number) evaluatedValue);
                }
            }
            return sum;
        } else {
            return 0;
        }
    }

    private static Number evaluateSubtractValue(Object expressionValue, Document document) throws MongoServerException {
        Object value = evaluate(expressionValue, document);
        if (!(value instanceof Collection)) {
            throw new MongoServerError(16020, "Expression $subtract takes exactly 2 arguments. 1 were passed in.");
        }
        Collection values = (Collection) value;
        if (values.size() != 2) {
            throw new MongoServerError(16020, "Expression $subtract takes exactly 2 arguments. " + values.size() + " were passed in.");
        }

        Iterator iterator = values.iterator();
        Object one = evaluate(iterator.next(), document);
        Object other = evaluate(iterator.next(), document);

        if (!(one instanceof Number && other instanceof Number)) {
            throw new MongoServerError(16556,
                "cant $subtract a " + one.getClass().getName() + " from a " + other.getClass().getName());
        }

        return Utils.subtractNumbers((Number) one, (Number) other);
    }
}