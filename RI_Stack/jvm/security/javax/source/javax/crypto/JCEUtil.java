/*
 * Copyright (c) 2000 The Legion Of The Bouncy Castle
 * (http://www.bouncycastle.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

/*
 * Changes from the original sources include:
 *
 *   + The addition of the `count' variable in getImplementation, which
 *     serves as a sort of "safety net".
 *
 * -- Casey Marshall <rsdio@metastatic.org> 2002
 */

package javax.crypto;

import java.security.Security;
import java.security.Provider;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;

class JCEUtil
{
    /**
     * The maximum number of aliases we try before giving up. Anything
     * greater than this likely indicates a broken provider.
     */
    private static final int MAX_ALIASES = 10;

    static class Implementation
    {
        Object      engine;
        Provider    provider;

        Implementation(
            Object      engine,
            Provider    provider)
        {
            this.engine = engine;
            this.provider = provider;
        }

        Object getEngine()
        {
            return engine;
        }

        Provider getProvider()
        {
            return provider;
        }
    }

    /**
     * see if we can find an algorithm (or its alias and what it represents) in
     * the property table for the given provider.
     *
     * @return null if no algorithm found, an Implementation if it is.
     */
    static Implementation getImplementationFromProvider(
        String      baseName,
        String      algorithm,
        Provider    prov)
    {
        String      alias;
        int         count = 0;

        while ((alias = prov.getProperty("Alg.Alias." + baseName + "." + algorithm)) != null)
        {
            algorithm = alias;
            if (++count > MAX_ALIASES) {
                // Too many aliases. Goodbye!
                return null;
            }
        }

        String      className = prov.getProperty(baseName + "." + algorithm);

        if (className != null)
        {
            try
            {
                Class       cls;
                ClassLoader clsLoader = prov.getClass().getClassLoader();

                if (clsLoader != null)
                {
                    cls = clsLoader.loadClass(className);
                }
                else
                {
                    cls = Class.forName(className);
                }

                return new Implementation(cls.newInstance(), prov);
            }
            catch (ClassNotFoundException e)
            {
                throw new IllegalStateException(
                    "algorithm " + algorithm + " in provider " + prov.getName() + " but no class \"" + className + "\" found!");
            }
            catch (Exception e)
            {
e.printStackTrace();
                throw new IllegalStateException(
                    "algorithm " + algorithm + " in provider " + prov.getName() + " but class \"" + className + "\" inaccessible!");
            }
        }

        return null;
    }

    /**
     * return an implementation for a given algorithm/provider.
     * If the provider is null, we grab the first avalaible who has the required algorithm.
     *
     * @return null if no algorithm found, an Implementation if it is.
     * @exception NoSuchProviderException if a provider is specified and not found.
     */
    static Implementation getImplementation(
        String      baseName,
        String      algorithm,
        String      provider)
        throws NoSuchProviderException
    {
        if (provider == null)
        {
            Provider[] prov = Security.getProviders();

            //
            // search every provider looking for the algorithm we want.
            //
            for (int i = 0; i != prov.length; i++)
            {
                //
                // try case insensitive
                //
                Implementation imp = getImplementationFromProvider(baseName, algorithm.toUpperCase(), prov[i]);
                if (imp != null)
                {
                    return imp;
                }

                imp = getImplementationFromProvider(baseName, algorithm, prov[i]);
                if (imp != null)
                {
                    return imp;
                }
            }
        }
        else
        {
            Provider prov = Security.getProvider(provider);

            if (prov == null)
            {
                throw new NoSuchProviderException("Provider " + provider + " not found");
            }

            //
            // try case insensitive
            //
            Implementation imp = getImplementationFromProvider(baseName, algorithm.toUpperCase(), prov);
            if (imp != null)
            {
                return imp;
            }

            return getImplementationFromProvider(baseName, algorithm, prov);
        }

        return null;
    }
}
