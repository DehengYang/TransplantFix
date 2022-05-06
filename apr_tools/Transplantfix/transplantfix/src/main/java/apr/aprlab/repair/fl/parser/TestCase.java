package apr.aprlab.repair.fl.parser;

public class TestCase {

    private String testName;

    private boolean passed;

    public TestCase(String testName, boolean passed) {
        this.testName = testName;
        this.passed = passed;
    }

    public String getTestName() {
        return testName;
    }

    public boolean isPassed() {
        return passed;
    }

    public boolean isSuccessful() {
        return passed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (passed ? 1231 : 1237);
        result = prime * result + ((testName == null) ? 0 : testName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TestCase other = (TestCase) obj;
        if (passed != other.passed)
            return false;
        if (testName == null) {
            if (other.testName != null)
                return false;
        } else if (!testName.equals(other.testName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TestCase [testName=" + testName + ", passed=" + passed + "]";
    }
}
