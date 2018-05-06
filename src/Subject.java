public class Subject {
    private String abbrev;
    private String name;

    public Subject(String abbrev, String name){
        this.abbrev = abbrev;
        this.name = name;
    }

    public String getAbbrev() {
        return abbrev;
    }

    public String getName() {
        return name;
    }
}