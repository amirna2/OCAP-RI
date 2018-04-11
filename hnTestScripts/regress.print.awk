BEGIN   {
            fmt = "%-" maxLen "s %s\n";
        }

        {
            field1 = $1;
            field2 = $2 == "MULTIPLE" ? gensub(/,$/, "", "", gensub(/[A-Za-z0-9-]+:/, "", "g", $3)) : $2;

            printf fmt, field1, field2;
        }
