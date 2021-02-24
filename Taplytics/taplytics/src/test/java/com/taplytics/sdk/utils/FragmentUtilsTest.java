package com.taplytics.sdk.utils;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import com.taplytics.sdk.utils.FragmentUtils.TLGlobalLayoutListener;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Created by taplytics on 2017-09-20.
 */

public class FragmentUtilsTest {

    private ViewPager mockViewPager;
    private ViewGroup mockViewGroup;
    private PagerAdapter mockPagerAdapter;

    private TLGlobalLayoutListener globalLayoutListener;
    private TLGlobalLayoutListener nullGlobalLayoutListener;

    @Before
    public void setup() {
        mockViewPager = mock(ViewPager.class);
        mockViewGroup = mock(ViewGroup.class);
        mockPagerAdapter = mock(PagerAdapter.class);
        globalLayoutListener = new TLGlobalLayoutListener(mockViewPager, mockViewGroup, mockPagerAdapter);
        nullGlobalLayoutListener = new TLGlobalLayoutListener(null, null, null);
    }

    @Test
    public void testViewPagerGetter() {
        final ViewPager viewPager = globalLayoutListener.getViewPager();
        assertTrue(viewPager == mockViewPager);
    }

    @Test
    public void testViewPagerGetterNull() {
        final ViewPager viewPager = nullGlobalLayoutListener.getViewPager();
        assertTrue(viewPager == null);
    }

    @Test
    public void testViewGroupGetter() {
        final ViewGroup viewGroup = globalLayoutListener.getViewGroup();
        assertTrue(viewGroup == mockViewGroup);
    }

    @Test
    public void testViewGroupGetterNull() {
        final ViewGroup viewGroup = nullGlobalLayoutListener.getViewGroup();
        assertTrue(viewGroup == null);
    }

    @Test
    public void testPagerAdapterGetter() {
        final PagerAdapter pagerAdapter = globalLayoutListener.getPagerAdapter();
        assertTrue(pagerAdapter == mockPagerAdapter);
    }

    @Test
    public void testPagerAdapterGetterNull() {
        final PagerAdapter pagerAdapter = nullGlobalLayoutListener.getPagerAdapter();
        assertTrue(pagerAdapter == null);
    }

    @Test
    public void testHasViewPagerAndViewGroup() {
        assertTrue(globalLayoutListener.hasViewPagerAndViewGroup());
    }

    @Test
    public void testDoesNotHaveViewPagerAndViewGroup() {
        assertTrue(!nullGlobalLayoutListener.hasViewPagerAndViewGroup());
    }

}
