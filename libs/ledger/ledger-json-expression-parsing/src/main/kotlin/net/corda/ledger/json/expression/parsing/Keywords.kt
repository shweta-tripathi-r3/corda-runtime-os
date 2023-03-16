package net.corda.ledger.json.expression.parsing

class NotEquals : Keyword {
    override fun toString(): String {
        return "!="
    }
}

class And : Keyword {
    override fun toString(): String {
        return "AND"
    }
}

class Or : Keyword {
    override fun toString(): String {
        return "OR"
    }
}

class IsNull : Keyword {
    override fun toString(): String {
        return "IS NULL"
    }
}

class IsNotNull : Keyword {
    override fun toString(): String {
        return "IS NOT NULL"
    }
}

class GreaterThan : Keyword {
    override fun toString(): String {
        return ">"
    }
}

class GreaterThanEquals : Keyword {
    override fun toString(): String {
        return ">="
    }
}

class LessThan : Keyword {
    override fun toString(): String {
        return "<"
    }
}

class LessThanEquals : Keyword {
    override fun toString(): String {
        return "<="
    }
}

class JsonArrayOrObjectAsText : Keyword {
    override fun toString(): String {
        return "->>"
    }
}

class As : Keyword {
    override fun toString(): String {
        return "AS"
    }
}

class From : Keyword {
    override fun toString(): String {
        return "FROM"
    }
}

class Select : Keyword {
    override fun toString(): String {
        return "SELECT"
    }
}

class Where : Keyword {
    override fun toString(): String {
        return "WHERE"
    }
}

class Equals : Keyword {
    override fun toString(): String {
        return "="
    }
}

class In : Keyword {
    override fun toString(): String {
        return "IN"
    }
}