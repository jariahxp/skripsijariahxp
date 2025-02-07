package com.jariahxp.ui.dashboard.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.text.style.LeadingMarginSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.jariahxp.R
import com.jariahxp.databinding.FragmentAboutUsBinding

class AboutUsFragment : Fragment(R.layout.fragment_about_us) {

    private var _binding: FragmentAboutUsBinding? = null
    private val binding get() = _binding!!
    private var developerVisible = false
    private var latarBelakangVisible = false
    private var deskripsiVisible = false
    private var fiturVisible = false
    private var kontakVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutUsBinding.inflate(inflater, container, false)

        binding.apply {


            val indentSize = 50 // Indentasi dalam piksel


            developerView.text = SpannableString(getString(R.string.tentang_developer)).apply {
                setSpan(LeadingMarginSpan.Standard(indentSize, 0), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            latarbelakangView.text = SpannableString(getString(R.string.latar_belakang_aplikasi)).apply {
                setSpan(LeadingMarginSpan.Standard(indentSize, 0), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            deskripsiView.text = SpannableString(getString(R.string.deskripsi_aplikasi)).apply {
                setSpan(LeadingMarginSpan.Standard(indentSize, 0), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            fiturView.text = SpannableString(getString(R.string.fitur_aplikasi)).apply {
                setSpan(LeadingMarginSpan.Standard(indentSize, 0), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            kontakView.text = SpannableString(getString(R.string.kontak_kami)).apply {
                setSpan(LeadingMarginSpan.Standard(indentSize, 0), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            viewDeveloper.setOnClickListener {
                if (developerVisible) {

                    animateViewVisibility(viewDeveloper1, false)
                    animateViewVisibility(cardDeveloper, false )
                    animateViewVisibility(developerView, false)

                    animateIcon(viewDeveloper, R.drawable.baseline_arrow_drop_down_24)
                    developerVisible = false
                } else {
                    developerImage.visibility = View.VISIBLE

                    animateViewVisibility(viewDeveloper1, true, true)
                    animateViewVisibility(developerView, true,true)
                    animateViewVisibility(cardDeveloper, true ,true)

                    animateIcon(viewDeveloper, R.drawable.baseline_arrow_drop_up_24)
                    developerVisible = true
                }
            }

            viewLatarBelakang.setOnClickListener {
                if (latarBelakangVisible) {
                    animateViewVisibility(latarbelakangView, false)
                    animateIcon(viewLatarBelakang, R.drawable.baseline_arrow_drop_down_24)
                    latarBelakangVisible = false
                } else {
                    animateViewVisibility(latarbelakangView, true)
                    animateIcon(viewLatarBelakang, R.drawable.baseline_arrow_drop_up_24)
                    latarBelakangVisible = true
                }
            }

            viewDeskripsi.setOnClickListener {
                if (deskripsiVisible) {
                    animateViewVisibility(deskripsiView, false)
                    animateIcon(viewDeskripsi, R.drawable.baseline_arrow_drop_down_24)
                    deskripsiVisible = false
                } else {
                    animateViewVisibility(deskripsiView, true)
                    animateIcon(viewDeskripsi, R.drawable.baseline_arrow_drop_up_24)
                    deskripsiVisible = true
                }
            }

            viewFitur.setOnClickListener {
                if (fiturVisible) {
                    animateViewVisibility(fiturView, false)
                    animateIcon(viewFitur, R.drawable.baseline_arrow_drop_down_24)
                    fiturVisible = false
                } else {
                    animateViewVisibility(fiturView, true)
                    animateIcon(viewFitur, R.drawable.baseline_arrow_drop_up_24)
                    fiturVisible = true
                }
            }

            viewKontak.setOnClickListener {
                if (kontakVisible) {
                    animateViewVisibility(kontakView, false)
                    animateIcon(viewKontak, R.drawable.baseline_arrow_drop_down_24)
                    kontakVisible = false
                } else {
                    animateViewVisibility(kontakView, true)
                    animateIcon(viewKontak, R.drawable.baseline_arrow_drop_up_24)
                    kontakVisible = true
                }
            }

        }

        return binding.root
    }

    private fun animateViewVisibility(view: View, isVisible: Boolean, isDeveloper: Boolean = false) {
        if (isVisible) {
            // Pastikan visibilitas diatur ke INVISIBLE untuk menghitung ukuran penuh
            view.visibility = View.INVISIBLE
            view.measure(
                View.MeasureSpec.makeMeasureSpec((view.parent as View).width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.visibility = View.VISIBLE
            val targetHeight = view.measuredHeight

            // Hentikan animasi lama jika ada
            val currentAnimator = view.getTag(R.id.current_animator) as? ValueAnimator
            currentAnimator?.cancel()

            // Animasi expand
            val animator = ValueAnimator.ofInt(0, targetHeight).apply {
                if (isDeveloper) {
                    duration = 1000
                }else{
                    duration = 500
                }
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    val layoutParams = view.layoutParams
                    layoutParams.height = value
                    view.layoutParams = layoutParams
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Atur kembali height ke WRAP_CONTENT setelah animasi selesai
                        view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                        view.requestLayout()
                    }
                })
            }

            // Simpan referensi animasi ke tag view untuk menghindari konflik
            view.setTag(R.id.current_animator, animator)
            animator.start()

            // Tambahkan animasi alpha untuk efek fade-in
            view.alpha = 0f
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                if (isDeveloper) {
                    duration = 1000
                }else{
                    duration = 500
                }
            }.start()

            view.visibility = View.VISIBLE
        } else {
            val initialHeight = view.measuredHeight

            // Animasi collapse
            val animator = ValueAnimator.ofInt(initialHeight, 0).apply {
                if (isDeveloper) {
                    duration = 1000
                }else{
                    duration = 500
                }
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    val layoutParams = view.layoutParams
                    layoutParams.height = value
                    view.layoutParams = layoutParams
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Sembunyikan view setelah animasi selesai
                        view.visibility = View.GONE
                    }
                })
            }

            // Tambahkan animasi alpha untuk efek fade-out
            ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
                duration = 500
            }.start()

            animator.start()
        }
    }



    private fun animateIcon(imageView: ImageView, newResource: Int) {
        val outAnimator = ObjectAnimator.ofFloat(imageView, "rotationY", 0f, 90f).apply {
            duration = 150
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    imageView.setImageResource(newResource)
                    ObjectAnimator.ofFloat(imageView, "rotationY", -90f, 0f).apply {
                        duration = 150
                        start()
                    }
                }
            })
        }
        outAnimator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Set binding ke null untuk mencegah memory leaks
        _binding = null
    }
}
