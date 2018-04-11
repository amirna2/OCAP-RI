    function processTest()
    {
        if (maturity >= minMaturity && ! attended)
        {
            split(fileName, tFields, "/");
            sub("tset", "tset.builder", tFields[2]);
            split(tFields[4], tSet, ".");
            print tFields[2] "-" tFields[3] "-" tSet[1] "-" tpNum
        }
    }

BEGIN \
    {
        first = 1;
    }

/^>># MATURITY / \
    {
        if (! first)
        {
            processTest()
        }

        first = 0;

        maturity = $3;
        attended = 0;
    }

/^>>ASSERTION / \
    {
        tpNum = $2;
    }

/^>>PRAGMA attended/ \
    {
        attended = 1;
    }

END \
    {
        if (! first)
        {
            processTest()
        }
    }
