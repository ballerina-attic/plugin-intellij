function main (string[] args) {
    Con c = create Con();
    Con.test(c,<caret>)
}

connector Con () {

    action test(string a, string b){

    }
}