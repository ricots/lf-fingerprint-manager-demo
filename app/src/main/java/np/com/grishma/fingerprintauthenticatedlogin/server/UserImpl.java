package np.com.grishma.fingerprintauthenticatedlogin.server;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import np.com.grishma.fingerprintauthenticatedlogin.FingerprintAuthenticatedLogin;
import np.com.grishma.fingerprintauthenticatedlogin.MainActivity;

/**
 * An implementation on {@link User} to represent a User and its related tasks to be performed for User login via remote server
 */
public class UserImpl implements User {
    private static final String TAG = "UserImpl";
    private final Map<String, PublicKey> publicKeys = new HashMap<>();
    private final Set<String> receivedUsername = new HashSet<>();
    private SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(FingerprintAuthenticatedLogin.getContext());


    @Override
    public boolean verify(String username, byte[] usernameSignature) {
        try {
            receivedUsername.add(username);
//          For now fetch public key from the device itself as we have not used any database to
//          represent data being stored in server
//          PublicKey publicKey = publicKeys.get(username);
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PublicKey publicKey = keyStore.getCertificate(MainActivity.KEY_NAME).getPublicKey();

            Signature verificationFunction = Signature.getInstance("SHA256withECDSA");
            verificationFunction.initVerify(publicKey);
            verificationFunction.update(username.getBytes());
            if (verificationFunction.verify(usernameSignature)) {
                // Transaction is verified with the public key associated with the user
                // Do some post purchase processing in the server
                return true;
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            // In a real world, better to send some error message to the user
            Log.e(TAG, e.getMessage());
            return false;
        } catch (KeyStoreException | CertificateException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean verify(String username, String password) {
        String passwordTemp = sharedPreferences.getString(username, null);

        // store the password under the key "username" if there is no existing data
        // as for now we are assuming any new username and password is valid
        // and also store new username and password
        // if there is existing, then compare the password with existing
        if (passwordTemp == null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(username, password);
            editor.apply();
        }
        return passwordTemp == null || passwordTemp.equals(password);
    }

    @Override
    public boolean enroll(String username, String password, PublicKey publicKey) {
        if (publicKey != null) {
            publicKeys.put(username, publicKey);
        }
        // We just ignore the provided password here, but in real life, it is registered to the
        // backend.
        return true;
    }

    @Override
    public boolean resetPasswordViaServer(String username, String newPassword) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(username, newPassword); // save password in value, username is used as key
        editor.apply();
        return true;
    }
}
