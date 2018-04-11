BEGIN   {
            maxlen = 0;
        }

        {
            if (length ($1) > maxLen)
            {
                maxLen = length ($1);
            }
        }

END     {
            print maxLen;
        }
