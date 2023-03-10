package net.corda.ledger.json.expression.parsing

class EqualsOperator : Operator

class NotEqualsOperator : Operator

class LogicalAndOperator : Operator

class LogicalOrOperator : Operator

class LogicalNotOperator : Operator

class JsonArrayOrObjectAsText : Operator {
    override fun toString(): String {
        return "->>"
    }
}

class JsonAsNamedField : Operator {
    override fun toString(): String {
        return "AS"
    }
}

class JsonFrom : Operator {
    override fun toString(): String {
        return "FROM"
    }
}

class JsonSelect : Operator {
    override fun toString(): String {
        return "SELECT"
    }
}

class JsonWhere : Operator {
    override fun toString(): String {
        return "WHERE"
    }
}

class JsonEqualTo : Operator {
    override fun toString(): String {
        return "="
    }
}