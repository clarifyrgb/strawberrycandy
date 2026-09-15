path = "app/src/main/java/com/example/data/UploadNovelResult.kt"
with open(path, "r") as f:
    content = f.read()

target = """    val hasExistingAccount = !rememberedPass.isNullOrBlank()
    try {
      if (isSignUp) {
        val fbRes = firebaseAuthManager.signUp(cleanEmail, cleanPass)
        if (fbRes is com.example.data.auth.FirebaseAuthResult.Error) {
          android.util.Log.w("StrawberrycandyRepository", "Firebase sign up note: ${fbRes.message}, proceeding locally")
        }
      } else {
        val fbRes = firebaseAuthManager.signIn(cleanEmail, cleanPass)
        if (fbRes is com.example.data.auth.FirebaseAuthResult.Error) {
          android.util.Log.w("StrawberrycandyRepository", "Firebase sign in note: ${fbRes.message}, proceeding locally")
        }
      }
    } catch (e: Exception) {
      android.util.Log.w("StrawberrycandyRepository", "Firebase auth exception handled: ${e.message}, proceeding locally")
    }"""

replacement = """    val hasExistingAccount = !rememberedPass.isNullOrBlank()
    if (isSignUp && hasExistingAccount && rememberedPass != cleanPass) {
      return Result.failure(
        IllegalArgumentException("An account already exists for $cleanEmail. Please switch to 'Sign In' and enter your existing password, or use 'Forgot Password?' to reset it.")
      )
    }
    if (isSignUp) {
      val fbRes = firebaseAuthManager.signUp(cleanEmail, cleanPass)
      if (fbRes is com.example.data.auth.FirebaseAuthResult.Error) {
        return Result.failure(IllegalArgumentException(fbRes.message))
      }
    } else {
      val fbRes = firebaseAuthManager.signIn(cleanEmail, cleanPass)
      if (fbRes is com.example.data.auth.FirebaseAuthResult.Error) {
        return Result.failure(IllegalArgumentException(fbRes.message))
      }
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(path, "w") as f:
        f.write(content)
    print("Successfully replaced!")
else:
    print("Target not found!")
