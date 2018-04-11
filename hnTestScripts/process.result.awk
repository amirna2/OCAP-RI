        {
            field1 = $1;
            field2 = $2;
            field3 = $3 == "MULTIPLE" ? gensub(/,$/, "", "", gensub(/[A-Za-z0-9-]+:/, "", "g", $4)) : $3;
            field4 = field3 ~ /^PASS(,PASS)*$/ ? "PASS" : "NONPASS";

            print field1, field2, field4;
        }
