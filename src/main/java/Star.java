package main.java;

public class Star {
    private String stageName;
    private String id;
    private Integer birthYear;

    public Star() { }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }

    public String toString() {

        return "Name: " + getStageName() + ", " +
                "ID: " + getId() + ", " +
                "Birth Year: " + getBirthYear();
    }
}
