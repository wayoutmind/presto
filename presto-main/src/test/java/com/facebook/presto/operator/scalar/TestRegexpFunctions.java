/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.operator.scalar;

import com.facebook.presto.spi.PrestoException;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.type.ArrayType;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.presto.spi.StandardErrorCode.INVALID_FUNCTION_ARGUMENT;
import static com.facebook.presto.spi.type.BooleanType.BOOLEAN;
import static com.facebook.presto.spi.type.VarcharType.VARCHAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class TestRegexpFunctions
{
    private FunctionAssertions functionAssertions;

    @BeforeClass
    public void setUp()
    {
        functionAssertions = new FunctionAssertions();
    }

    @Test
    public void testRegexpLike()
    {
        assertFunction("REGEXP_LIKE('Stephen', 'Ste(v|ph)en')", BOOLEAN, true);
        assertFunction("REGEXP_LIKE('Stevens', 'Ste(v|ph)en')", BOOLEAN, true);
        assertFunction("REGEXP_LIKE('Stephen', '^Ste(v|ph)en$')", BOOLEAN, true);
        assertFunction("REGEXP_LIKE('Stevens', '^Ste(v|ph)en$')", BOOLEAN, false);

        assertFunction("REGEXP_LIKE('hello world', '[a-z]')", BOOLEAN, true);
        assertFunction("REGEXP_LIKE('Hello', '^[a-z]+$')", BOOLEAN, false);
        assertFunction("REGEXP_LIKE('Hello', '^(?i)[a-z]+$')", BOOLEAN, true);
        assertFunction("REGEXP_LIKE('Hello', '^[a-zA-Z]+$')", BOOLEAN, true);
    }

    @Test
    public void testRegexpReplace()
    {
        assertFunction("REGEXP_REPLACE('fun stuff.', '[a-z]')", VARCHAR, " .");
        assertFunction("REGEXP_REPLACE('fun stuff.', '[a-z]', '*')", VARCHAR, "*** *****.");

        assertFunction(
                "REGEXP_REPLACE('call 555.123.4444 now', '(\\d{3})\\.(\\d{3}).(\\d{4})')",
                VARCHAR,
                "call  now");
        assertFunction(
                "REGEXP_REPLACE('call 555.123.4444 now', '(\\d{3})\\.(\\d{3}).(\\d{4})', '($1) $2-$3')",
                VARCHAR,
                "call (555) 123-4444 now");
    }

    @Test
    public void testRegexpExtract()
    {
        assertFunction("REGEXP_EXTRACT('Hello world bye', '\\b[a-z]([a-z]*)')", VARCHAR, "world");
        assertFunction("REGEXP_EXTRACT('Hello world bye', '\\b[a-z]([a-z]*)', 1)", VARCHAR, "orld");
        assertFunction("REGEXP_EXTRACT('rat cat\nbat dog', 'ra(.)|blah(.)(.)', 2)", VARCHAR, null);
    }

    @Test
    public void testRegexpExtractAll()
    {
        assertFunction("REGEXP_EXTRACT_ALL('rat cat\nbat dog', '.at')", new ArrayType(VARCHAR), ImmutableList.of("rat", "cat", "bat"));
        assertFunction("REGEXP_EXTRACT_ALL('rat cat\nbat dog', '(.)at', 1)", new ArrayType(VARCHAR), ImmutableList.of("r", "c", "b"));
        List<String> nullList = new ArrayList<>();
        nullList.add(null);
        assertFunction("REGEXP_EXTRACT_ALL('rat cat\nbat dog', 'ra(.)|blah(.)(.)', 2)", new ArrayType(VARCHAR), nullList);
        assertInvalidFunction("REGEXP_EXTRACT_ALL('hello', '(.)', 2)", new ArrayType(VARCHAR), "Pattern has 1 groups. Cannot access group 2");
    }

    private void assertFunction(String projection, Type expectedType, Object expected)
    {
        functionAssertions.assertFunction(projection, expectedType, expected);
    }

    private void assertInvalidFunction(String projection, Type expectedType, String message)
    {
        try {
            assertFunction(projection, expectedType, null);
            fail("Expected to throw an INVALID_FUNCTION_ARGUMENT exception with message " + message);
        }
        catch (PrestoException e) {
            assertEquals(e.getErrorCode(), INVALID_FUNCTION_ARGUMENT.toErrorCode());
            assertEquals(e.getMessage(), message);
        }
    }
}
