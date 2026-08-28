package aethereal.friend;


public class FriendConstructor {
    private String a;
    private String note;
    private boolean favorite;

    public FriendConstructor() {
        this.note = "";
    }

    public FriendConstructor(String name) {
        this(name, "", false);
    }

    public FriendConstructor(String name, String note, boolean favorite) {
        this.a = name;
        this.note = note == null ? "" : note;
        this.favorite = favorite;
    }

    public void a(String name) {
        this.a = name;
    }

    public String a() {
        return this.a;
    }

    public String note() {
        return this.note == null ? "" : this.note;
    }

    public void note(String note) {
        this.note = note == null ? "" : note;
    }

    public boolean favorite() {
        return this.favorite;
    }

    public void favorite(boolean favorite) {
        this.favorite = favorite;
    }
}
