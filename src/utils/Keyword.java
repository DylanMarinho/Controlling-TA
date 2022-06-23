package utils;

public enum Keyword {
    CLOCK {
        @Override
        public String toString() {
            return "clock";
        }
    },
    PARAMETER {
        @Override
        public String toString() {
            return "parameter";
        }
    },
    BOOL {
        @Override
        public String toString() {
            return "bool";
        }
    },
    SYNCLABS {
        @Override
        public String toString() {
            return "synclabs";
        }
    },
    CONTINUOUS {
        @Override
        public String toString() {
            return "continuous";
        }
    },
    DISCRETE {
        @Override
        public String toString() {
            return "discrete";
        }
    },
    AUTOMATON {
        @Override
        public String toString() {
            return "automaton";
        }
    },
    SYNC {
        @Override
        public String toString() {
            return "sync";
        }
    },
    DO {
        @Override
        public String toString() {
            return "do";
        }
    },
    T_ABS {
        @Override
        public String toString() {
            return "t_abs";
        }
    },
    P_ABS {
        @Override
        public String toString() {
            return "p_abs";
        }
    },
    VISITED_PRIV {
        @Override
        public String toString() {
            return "visited_qpriv";
        }
    },
    GOTO{
        @Override
        public String toString() {
            return "goto";
        }
    },
    LOC_FINAL{
        @Override
        public String toString() {
            return "qf";
        }
    },
    LOC_PRIV{
        @Override
        public String toString() {
            return "qpriv";
        }
    }
}
