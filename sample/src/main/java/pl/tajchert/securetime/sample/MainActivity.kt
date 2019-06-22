package pl.tajchert.securetime.sample

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import pl.tajchert.securetime.SecureTime
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    val disposable = CompositeDisposable()

    private var roughtimeHelper: SecureTime? = SecureTime(
        host = "roughtime.cloudflare.com",
        port = 2002,
        serverPubKeyBase64 = "gD63hSj3ScS+wuOeGrubXlq35N1c5Lby/S+T7MNTjxo="
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttonRefresh.setOnClickListener {
            getCurrentTime()
        }
    }

    override fun onResume() {
        super.onResume()
        getCurrentTime()
    }

    @SuppressLint("CheckResult")
    private fun getCurrentTime() {
        disposable.clear()

        disposable.add(Observable.fromCallable {
            if (roughtimeHelper == null) {
                Timber.e("Error while initializing SecureTime - most likely serverPubKey")
                throw RuntimeException("SecureTime is not initialized")
            } else {
                roughtimeHelper!!.getTime()
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                //TODO show processing indicator
                Timber.d("Starting fetching time")

                timeServer.text = "Server midpoint time\nX"
                timeServerRadius.text = "Uncertainty radius  X ms"
                timeDiffMilliseconds.text = "Time diff X ms"
                timeDiffSeconds.text = "Time diff X s"
            }.doOnComplete {
                //TODO hide pricessing indicator
                Timber.d("Fetched completed")
            }
            .subscribe(
                { result ->
                    val midpoint = result.first
                    val radiousOfUncertanity = result.second
                    val localTimeDiffMilliseconds = result.third
                    val localTimeDiffSeconds = TimeUnit.MILLISECONDS.toSeconds(result.third)
                    //Use result for something
                    Timber.d("Result from server, midpoint: $midpoint, radius(ms): $radiousOfUncertanity, localDiff (ms): $localTimeDiffMilliseconds")
                    timeServer.text = "Server midpoint time:\n$midpoint"
                    timeServerRadius.text = "Uncertainty radius  $radiousOfUncertanity ms"
                    timeDiffMilliseconds.text = "Time diff $localTimeDiffMilliseconds ms"
                    timeDiffSeconds.text = "Time diff $localTimeDiffSeconds s"
                },
                {
                    Timber.e(it)
                    Toast.makeText(MainActivity@ this, "Error: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                })
        )
    }
}
